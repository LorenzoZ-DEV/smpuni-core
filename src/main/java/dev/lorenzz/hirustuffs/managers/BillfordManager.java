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

public class BillfordManager {

    private static BillfordManager INSTANCE;

    private final Hiru plugin;
    private final List<BillfordTrade> allTrades = new ArrayList<>();
    private List<BillfordTrade> activeTrades = new ArrayList<>();
    private long lastRotation;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public record BillfordTrade(Material inputMaterial, int inputAmount,
                                 Material outputMaterial, int outputAmount, String displayName) {}

    public BillfordManager(Hiru plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static BillfordManager getInstance() { return INSTANCE; }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/billford.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                createDefaults();
            }
        } catch (IOException e) { e.printStackTrace(); }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadTrades();
        rotateTrades();
    }

    private void createDefaults() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("trades.trade1.input.material", "DIAMOND");
        defaults.set("trades.trade1.input.amount", 3);
        defaults.set("trades.trade1.output.material", "NETHERITE_SCRAP");
        defaults.set("trades.trade1.output.amount", 1);
        defaults.set("trades.trade1.display-name", "&#FFD700Diamond → Netherite Scrap");

        defaults.set("trades.trade2.input.material", "EMERALD");
        defaults.set("trades.trade2.input.amount", 16);
        defaults.set("trades.trade2.output.material", "TOTEM_OF_UNDYING");
        defaults.set("trades.trade2.output.amount", 1);
        defaults.set("trades.trade2.display-name", "&#55FF55Emeralds → Totem");

        defaults.set("trades.trade3.input.material", "IRON_INGOT");
        defaults.set("trades.trade3.input.amount", 32);
        defaults.set("trades.trade3.output.material", "GOLDEN_APPLE");
        defaults.set("trades.trade3.output.amount", 4);
        defaults.set("trades.trade3.display-name", "&#FFD700Iron → Golden Apples");

        defaults.set("trades.trade4.input.material", "BLAZE_ROD");
        defaults.set("trades.trade4.input.amount", 8);
        defaults.set("trades.trade4.output.material", "ENDER_PEARL");
        defaults.set("trades.trade4.output.amount", 4);
        defaults.set("trades.trade4.display-name", "&#4FC3F7Blaze Rods → Ender Pearls");

        defaults.set("trades.trade5.input.material", "AMETHYST_SHARD");
        defaults.set("trades.trade5.input.amount", 64);
        defaults.set("trades.trade5.output.material", "ENCHANTED_GOLDEN_APPLE");
        defaults.set("trades.trade5.output.amount", 1);
        defaults.set("trades.trade5.display-name", "&#CE93D8Amethyst → God Apple");

        defaults.set("rotation.last", 0L);
        defaults.set("rotation.active", List.of());

        try { defaults.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadTrades() {
        allTrades.clear();
        ConfigurationSection sec = dataConfig.getConfigurationSection("trades");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection ts = sec.getConfigurationSection(key);
            if (ts == null) continue;
            Material inMat = Material.matchMaterial(ts.getString("input.material", "STONE"));
            int inAmt = ts.getInt("input.amount", 1);
            Material outMat = Material.matchMaterial(ts.getString("output.material", "STONE"));
            int outAmt = ts.getInt("output.amount", 1);
            String display = ts.getString("display-name", key);
            if (inMat != null && outMat != null) {
                allTrades.add(new BillfordTrade(inMat, inAmt, outMat, outAmt, display));
            }
        }
    }

    public void rotateTrades() {
        int rotationHours = plugin.getConfigFile().getInt("billford.rotation-hours", 6);
        long rotationMs = rotationHours * 3600000L;
        lastRotation = dataConfig.getLong("rotation.last", 0L);

        if (System.currentTimeMillis() - lastRotation > rotationMs || activeTrades.isEmpty()) {
            int tradeCount = Math.min(plugin.getConfigFile().getInt("billford.active-trades", 3), allTrades.size());
            List<BillfordTrade> shuffled = new ArrayList<>(allTrades);
            Collections.shuffle(shuffled);
            activeTrades = shuffled.subList(0, tradeCount);
            lastRotation = System.currentTimeMillis();

            dataConfig.set("rotation.last", lastRotation);
            List<String> activeNames = new ArrayList<>();
            for (BillfordTrade t : activeTrades) activeNames.add(t.displayName());
            dataConfig.set("rotation.active", activeNames);
            try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
        } else {
            // Load active from saved state if within rotation window
            // Just use the shuffled ones loaded above
        }
    }

    public List<BillfordTrade> getActiveTrades() { return Collections.unmodifiableList(activeTrades); }
    public List<BillfordTrade> getAllTrades() { return Collections.unmodifiableList(allTrades); }

    public long getNextRotation() {
        int rotationHours = plugin.getConfigFile().getInt("billford.rotation-hours", 6);
        long rotationMs = rotationHours * 3600000L;
        return lastRotation + rotationMs;
    }
}
