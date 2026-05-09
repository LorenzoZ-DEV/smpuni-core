package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
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

    private final Solum plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public record ShopItem(Material material, double buyPrice, double sellPrice, String currency) {}

    /**
     * Represents a custom item (spawners, crates, etc.) that executes a console command on purchase.
     * The command string supports {player} placeholder which is replaced with the buyer's name.
     * displayMaterial is used as the icon in the GUI.
     */
    public record ShopCommandItem(String id, String displayName, Material displayMaterial,
                                   double buyPrice, String currency, String command,
                                   List<String> lore) {}

    public record ShopCategory(String name, String displayName, Material icon,
                                List<ShopItem> items, List<ShopCommandItem> commandItems) {
        /** Backwards-compatible constructor for categories with no command items */
        public ShopCategory(String name, String displayName, Material icon, List<ShopItem> items) {
            this(name, displayName, icon, items, new ArrayList<>());
        }
    }

    public ShopManager(Solum plugin) {
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

        // ── Overworld Blocks ──
        defaults.set("categories.overworld.display-name", "&#55FF55&lOverworld");
        defaults.set("categories.overworld.icon", "GRASS_BLOCK");
        addItem(defaults, "overworld", "OAK_LOG", 10, 5, "MONEY");
        addItem(defaults, "overworld", "BIRCH_LOG", 10, 5, "MONEY");
        addItem(defaults, "overworld", "SPRUCE_LOG", 10, 5, "MONEY");
        addItem(defaults, "overworld", "DARK_OAK_LOG", 12, 6, "MONEY");
        addItem(defaults, "overworld", "COBBLESTONE", 5, 2, "MONEY");
        addItem(defaults, "overworld", "STONE", 8, 3, "MONEY");
        addItem(defaults, "overworld", "SAND", 5, 2, "MONEY");
        addItem(defaults, "overworld", "GRAVEL", 5, 2, "MONEY");
        addItem(defaults, "overworld", "CLAY_BALL", 8, 4, "MONEY");
        addItem(defaults, "overworld", "GLASS", 6, 3, "MONEY");
        addItem(defaults, "overworld", "OBSIDIAN", 75, 30, "MONEY");

        // ── Ores & Minerals ──
        defaults.set("categories.ores.display-name", "&#FFD700&lOres & Minerals");
        defaults.set("categories.ores.icon", "DIAMOND");
        addItem(defaults, "ores", "COAL", 15, 6, "MONEY");
        addItem(defaults, "ores", "IRON_INGOT", 50, 25, "MONEY");
        addItem(defaults, "ores", "GOLD_INGOT", 100, 50, "MONEY");
        addItem(defaults, "ores", "DIAMOND", 500, 250, "MONEY");
        addItem(defaults, "ores", "EMERALD", 400, 200, "MONEY");
        addItem(defaults, "ores", "LAPIS_LAZULI", 30, 15, "MONEY");
        addItem(defaults, "ores", "REDSTONE", 20, 10, "MONEY");
        addItem(defaults, "ores", "COPPER_INGOT", 15, 8, "MONEY");
        addItem(defaults, "ores", "QUARTZ", 25, 12, "MONEY");
        addItem(defaults, "ores", "AMETHYST_SHARD", 60, 30, "MONEY");
        addItem(defaults, "ores", "NETHERITE_INGOT", 5000, 2500, "MONEY");
        addItem(defaults, "ores", "ANCIENT_DEBRIS", 2500, 1250, "MONEY");

        // ── Farming ──
        defaults.set("categories.farming.display-name", "&#55FF55&lFarming");
        defaults.set("categories.farming.icon", "WHEAT");
        addItem(defaults, "farming", "WHEAT", 5, 2, "MONEY");
        addItem(defaults, "farming", "CARROT", 5, 2, "MONEY");
        addItem(defaults, "farming", "POTATO", 5, 2, "MONEY");
        addItem(defaults, "farming", "BEETROOT", 8, 3, "MONEY");
        addItem(defaults, "farming", "MELON_SLICE", 3, 1, "MONEY");
        addItem(defaults, "farming", "PUMPKIN", 15, 6, "MONEY");
        addItem(defaults, "farming", "SUGAR_CANE", 10, 4, "MONEY");
        addItem(defaults, "farming", "CACTUS", 8, 3, "MONEY");
        addItem(defaults, "farming", "SWEET_BERRIES", 6, 2, "MONEY");
        addItem(defaults, "farming", "COCOA_BEANS", 10, 4, "MONEY");
        addItem(defaults, "farming", "BAMBOO", 3, 1, "MONEY");
        addItem(defaults, "farming", "KELP", 3, 1, "MONEY");

        // ── Food ──
        defaults.set("categories.food.display-name", "&#FF8B76&lFood");
        defaults.set("categories.food.icon", "COOKED_BEEF");
        addItem(defaults, "food", "BREAD", 10, 4, "MONEY");
        addItem(defaults, "food", "COOKED_BEEF", 20, 10, "MONEY");
        addItem(defaults, "food", "COOKED_PORKCHOP", 20, 10, "MONEY");
        addItem(defaults, "food", "COOKED_CHICKEN", 15, 6, "MONEY");
        addItem(defaults, "food", "GOLDEN_APPLE", 1000, 500, "MONEY");
        addItem(defaults, "food", "GOLDEN_CARROT", 200, 100, "MONEY");
        addItem(defaults, "food", "CAKE", 80, 30, "MONEY");

        // ── Mob Drops ──
        defaults.set("categories.mob_drops.display-name", "&#FF4444&lMob Drops");
        defaults.set("categories.mob_drops.icon", "BONE");
        addItem(defaults, "mob_drops", "BONE", 15, 6, "MONEY");
        addItem(defaults, "mob_drops", "STRING", 10, 4, "MONEY");
        addItem(defaults, "mob_drops", "GUNPOWDER", 25, 10, "MONEY");
        addItem(defaults, "mob_drops", "SPIDER_EYE", 20, 8, "MONEY");
        addItem(defaults, "mob_drops", "ROTTEN_FLESH", 5, 2, "MONEY");
        addItem(defaults, "mob_drops", "LEATHER", 25, 10, "MONEY");
        addItem(defaults, "mob_drops", "SLIME_BALL", 40, 16, "MONEY");
        addItem(defaults, "mob_drops", "GHAST_TEAR", 250, 100, "MONEY");
        addItem(defaults, "mob_drops", "PHANTOM_MEMBRANE", 100, 40, "MONEY");
        addItem(defaults, "mob_drops", "INK_SAC", 15, 6, "MONEY");
        addItem(defaults, "mob_drops", "GLOW_INK_SAC", 50, 20, "MONEY");

        // ── Nether ──
        defaults.set("categories.nether.display-name", "&#FF4444&lNether");
        defaults.set("categories.nether.icon", "NETHERRACK");
        addItem(defaults, "nether", "BLAZE_ROD", 100, 50, "SHARDS");
        addItem(defaults, "nether", "NETHER_WART", 20, 10, "SHARDS");
        addItem(defaults, "nether", "MAGMA_CREAM", 60, 30, "SHARDS");
        addItem(defaults, "nether", "GHAST_TEAR", 200, 100, "SHARDS");
        addItem(defaults, "nether", "GLOWSTONE", 40, 20, "SHARDS");
        addItem(defaults, "nether", "CRIMSON_STEM", 20, 10, "SHARDS");
        addItem(defaults, "nether", "WARPED_STEM", 20, 10, "SHARDS");
        addItem(defaults, "nether", "CRYING_OBSIDIAN", 100, 50, "SHARDS");
        addItem(defaults, "nether", "WITHER_SKELETON_SKULL", 2000, 1000, "SHARDS");

        // ── End ──
        defaults.set("categories.end.display-name", "&#CE93D8&lThe End");
        defaults.set("categories.end.icon", "END_STONE");
        addItem(defaults, "end", "ENDER_PEARL", 200, 100, "SHARDS");
        addItem(defaults, "end", "END_STONE", 15, 6, "SHARDS");
        addItem(defaults, "end", "CHORUS_FRUIT", 40, 20, "SHARDS");
        addItem(defaults, "end", "PURPUR_BLOCK", 30, 15, "SHARDS");
        addItem(defaults, "end", "SHULKER_SHELL", 500, 250, "SHARDS");
        addItem(defaults, "end", "DRAGON_BREATH", 400, 200, "SHARDS");
        addItem(defaults, "end", "ELYTRA", 10000, 5000, "SHARDS");

        // ── Redstone ──
        defaults.set("categories.redstone.display-name", "&#FF4444&lRedstone");
        defaults.set("categories.redstone.icon", "REDSTONE");
        addItem(defaults, "redstone", "REDSTONE", 20, 10, "MONEY");
        addItem(defaults, "redstone", "REPEATER", 40, 20, "MONEY");
        addItem(defaults, "redstone", "COMPARATOR", 60, 30, "MONEY");
        addItem(defaults, "redstone", "PISTON", 80, 40, "MONEY");
        addItem(defaults, "redstone", "STICKY_PISTON", 120, 60, "MONEY");
        addItem(defaults, "redstone", "HOPPER", 150, 75, "MONEY");
        addItem(defaults, "redstone", "OBSERVER", 100, 50, "MONEY");
        addItem(defaults, "redstone", "TNT", 120, 60, "MONEY");
        addItem(defaults, "redstone", "DROPPER", 50, 25, "MONEY");
        addItem(defaults, "redstone", "DISPENSER", 60, 30, "MONEY");

        // ── Combat ──
        defaults.set("categories.combat.display-name", "&#FF6B6B&lCombat");
        defaults.set("categories.combat.icon", "DIAMOND_SWORD");
        addItem(defaults, "combat", "ARROW", 5, 2, "MONEY");
        addItem(defaults, "combat", "BOW", 100, 40, "MONEY");
        addItem(defaults, "combat", "CROSSBOW", 150, 60, "MONEY");
        addItem(defaults, "combat", "SHIELD", 80, 30, "MONEY");
        addItem(defaults, "combat", "TOTEM_OF_UNDYING", 5000, 2500, "SHARDS");
        addItem(defaults, "combat", "GOLDEN_APPLE", 1000, 500, "MONEY");
        addItem(defaults, "combat", "ENCHANTED_GOLDEN_APPLE", 25000, 12500, "SHARDS");

        // ── Custom Items (spawners, crates, etc.) ──
        defaults.set("categories.custom.display-name", "&#FF55FF&lCustom Items");
        defaults.set("categories.custom.icon", "SPAWNER");
        addCommandItem(defaults, "custom", "zombie_spawner", "&#55FF55Zombie Spawner",
                "SPAWNER", 50000, "MONEY", "silkspawners:ss give {player} ZOMBIE 1",
                List.of("&#8C8C8CA zombie mob spawner"));
        addCommandItem(defaults, "custom", "skeleton_spawner", "&#CCCCCC Skeleton Spawner",
                "SPAWNER", 50000, "MONEY", "silkspawners:ss give {player} SKELETON 1",
                List.of("&#8C8C8CA skeleton mob spawner"));
        addCommandItem(defaults, "custom", "common_crate", "&#55FF55Common Crate Key",
                "TRIPWIRE_HOOK", 5000, "MONEY", "crates givekey {player} common 1",
                List.of("&#8C8C8CA common crate key"));
        addCommandItem(defaults, "custom", "rare_crate", "&#5555FFRare Crate Key",
                "TRIPWIRE_HOOK", 15000, "MONEY", "crates givekey {player} rare 1",
                List.of("&#8C8C8CA rare crate key"));
        addCommandItem(defaults, "custom", "legendary_crate", "&#FFD700Legendary Crate Key",
                "TRIPWIRE_HOOK", 50000, "SHARDS", "crates givekey {player} legendary 1",
                List.of("&#8C8C8CA legendary crate key"));

        try { defaults.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void addItem(YamlConfiguration config, String category, String material, double buy, double sell, String currency) {
        String path = "categories." + category + ".items." + material;
        config.set(path + ".buy", buy);
        config.set(path + ".sell", sell);
        config.set(path + ".currency", currency);
    }

    private void addCommandItem(YamlConfiguration config, String category, String id,
                                 String displayName, String displayMaterial, double buy,
                                 String currency, String command, List<String> lore) {
        String path = "categories." + category + ".command-items." + id;
        config.set(path + ".display-name", displayName);
        config.set(path + ".display-material", displayMaterial);
        config.set(path + ".buy", buy);
        config.set(path + ".currency", currency);
        config.set(path + ".command", command);
        config.set(path + ".lore", lore);
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

            List<ShopCommandItem> commandItems = new ArrayList<>();
            ConfigurationSection cmdSec = cs.getConfigurationSection("command-items");
            if (cmdSec != null) {
                for (String cmdKey : cmdSec.getKeys(false)) {
                    ConfigurationSection cmdItem = cmdSec.getConfigurationSection(cmdKey);
                    if (cmdItem == null) continue;
                    String cmdDisplayName = cmdItem.getString("display-name", cmdKey);
                    Material displayMat = Material.matchMaterial(cmdItem.getString("display-material", "BARRIER"));
                    if (displayMat == null) displayMat = Material.BARRIER;
                    double buy = cmdItem.getDouble("buy", 0);
                    String currency = cmdItem.getString("currency", "MONEY");
                    String command = cmdItem.getString("command", "");
                    List<String> lore = cmdItem.getStringList("lore");
                    commandItems.add(new ShopCommandItem(cmdKey, cmdDisplayName, displayMat, buy, currency, command, lore));
                }
            }

            categories.put(catKey, new ShopCategory(catKey, displayName, icon, items, commandItems));
        }
        plugin.getLogger().info("[Solum] Loaded " + categories.size() + " shop categories.");
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
            for (ShopCommandItem cmdItem : cat.commandItems()) {
                String cPath = path + ".command-items." + cmdItem.id();
                dataConfig.set(cPath + ".display-name", cmdItem.displayName());
                dataConfig.set(cPath + ".display-material", cmdItem.displayMaterial().name());
                dataConfig.set(cPath + ".buy", cmdItem.buyPrice());
                dataConfig.set(cPath + ".currency", cmdItem.currency());
                dataConfig.set(cPath + ".command", cmdItem.command());
                dataConfig.set(cPath + ".lore", cmdItem.lore());
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("[Solum] Failed to save shop: " + e.getMessage());
        }
    }

    public Map<String, ShopCategory> getCategories() { return Collections.unmodifiableMap(categories); }
    public ShopCategory getCategory(String name) { return categories.get(name); }

    public void reload() { dataConfig = YamlConfiguration.loadConfiguration(dataFile); loadAll(); }
}
