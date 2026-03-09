package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ShopManager {

    private static ShopManager INSTANCE;

    private final Hiru plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public record ShopItem(Material material, double buyPrice, double sellPrice, String currency) {}
    public record ShopCategory(String name, String displayName, Material icon, List<ShopItem> items) {}

    public ShopManager(Hiru plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static ShopManager getInstance() { return INSTANCE; }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/shop.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                createDefaults();
            }
        } catch (IOException e) { e.printStackTrace(); }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void createDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        // Overworld
        defaults.set("categories.overworld.display-name", "&#55FF55&lOverworld");
        defaults.set("categories.overworld.icon", "GRASS_BLOCK");
        defaults.set("categories.overworld.items.OAK_LOG.buy", 10.0);
        defaults.set("categories.overworld.items.OAK_LOG.sell", 5.0);
        defaults.set("categories.overworld.items.OAK_LOG.currency", "MONEY");
        defaults.set("categories.overworld.items.COBBLESTONE.buy", 5.0);
        defaults.set("categories.overworld.items.COBBLESTONE.sell", 2.0);
        defaults.set("categories.overworld.items.COBBLESTONE.currency", "MONEY");
        defaults.set("categories.overworld.items.IRON_INGOT.buy", 50.0);
        defaults.set("categories.overworld.items.IRON_INGOT.sell", 25.0);
        defaults.set("categories.overworld.items.IRON_INGOT.currency", "MONEY");
        defaults.set("categories.overworld.items.DIAMOND.buy", 500.0);
        defaults.set("categories.overworld.items.DIAMOND.sell", 250.0);
        defaults.set("categories.overworld.items.DIAMOND.currency", "MONEY");

        // Nether
        defaults.set("categories.nether.display-name", "&#FF4444&lNether");
        defaults.set("categories.nether.icon", "NETHERRACK");
        defaults.set("categories.nether.items.BLAZE_ROD.buy", 100.0);
        defaults.set("categories.nether.items.BLAZE_ROD.sell", 50.0);
        defaults.set("categories.nether.items.BLAZE_ROD.currency", "SHARDS");
        defaults.set("categories.nether.items.NETHER_WART.buy", 20.0);
        defaults.set("categories.nether.items.NETHER_WART.sell", 10.0);
        defaults.set("categories.nether.items.NETHER_WART.currency", "SHARDS");

        // End
        defaults.set("categories.end.display-name", "&#CE93D8&lThe End");
        defaults.set("categories.end.icon", "END_STONE");
        defaults.set("categories.end.items.ENDER_PEARL.buy", 200.0);
        defaults.set("categories.end.items.ENDER_PEARL.sell", 100.0);
        defaults.set("categories.end.items.ENDER_PEARL.currency", "SHARDS");

        try { defaults.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadAll() {
        categories.clear();
        ConfigurationSection catSec = dataConfig.getConfigurationSection("categories");
        if (catSec == null) return;

        for (String catKey : catSec.getKeys(false)) {
            ConfigurationSection cs = catSec.getConfigurationSection(catKey);
            if (cs == null) continue;

            String displayName = cs.getString("display-name", catKey);
            Material icon = Material.matchMaterial(cs.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;

            List<ShopItem> items = new ArrayList<>();
            ConfigurationSection itemSec = cs.getConfigurationSection("items");
            if (itemSec != null) {
                for (String matKey : itemSec.getKeys(false)) {
                    Material mat = Material.matchMaterial(matKey);
                    if (mat == null) continue;
                    double buy = itemSec.getDouble(matKey + ".buy", 0);
                    double sell = itemSec.getDouble(matKey + ".sell", 0);
                    String currency = itemSec.getString(matKey + ".currency", "MONEY");
                    items.add(new ShopItem(mat, buy, sell, currency));
                }
            }
            categories.put(catKey, new ShopCategory(catKey, displayName, icon, items));
        }
        plugin.getLogger().info("[Hiru] Loaded " + categories.size() + " shop categories.");
    }

    public void saveAll() {
        dataConfig = new YamlConfiguration();
        for (Map.Entry<String, ShopCategory> entry : categories.entrySet()) {
            ShopCategory cat = entry.getValue();
            String path = "categories." + cat.name();
            dataConfig.set(path + ".display-name", cat.displayName());
            dataConfig.set(path + ".icon", cat.icon().name());
            for (ShopItem item : cat.items()) {
                String iPath = path + ".items." + item.material().name();
                dataConfig.set(iPath + ".buy", item.buyPrice());
                dataConfig.set(iPath + ".sell", item.sellPrice());
                dataConfig.set(iPath + ".currency", item.currency());
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("[Hiru] Failed to save shop: " + e.getMessage());
        }
    }

    public Map<String, ShopCategory> getCategories() { return Collections.unmodifiableMap(categories); }
    public ShopCategory getCategory(String name) { return categories.get(name); }

    public void reload() { dataConfig = YamlConfiguration.loadConfiguration(dataFile); loadAll(); }
}
