package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.stream.Collectors;

public class WorthManager {

    private static WorthManager INSTANCE;

    private final Hiru plugin;
    private final Map<Material, Double> prices = new LinkedHashMap<>();

    public WorthManager(Hiru plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static WorthManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        loadPrices();
    }

    public void loadPrices() {
        prices.clear();
        ConfigurationSection section = plugin.getConfigFile().getConfigurationSection("worth.items");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            Material mat = Material.matchMaterial(key);
            if (mat != null) {
                prices.put(mat, section.getDouble(key));
            }
        }
        plugin.getLogger().info("[Hiru] Loaded " + prices.size() + " item prices.");
    }

    public double getPrice(Material material) {
        return prices.getOrDefault(material, 0.0);
    }

    public boolean hasPrice(Material material) {
        return prices.containsKey(material);
    }

    public void setPrice(Material material, double price) {
        prices.put(material, price);
        plugin.getConfigFile().set("worth.items." + material.name(), price);
        plugin.getConfigFile().saveFile();
    }

    public Map<Material, Double> getAllPrices() {
        return Collections.unmodifiableMap(prices);
    }

    public List<Map.Entry<Material, Double>> getSortedPrices() {
        return prices.entrySet().stream()
                .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }
}
