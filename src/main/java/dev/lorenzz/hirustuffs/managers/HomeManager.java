package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HomeManager {

    private static HomeManager INSTANCE;

    private final Hiru plugin;
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public HomeManager(Hiru plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static HomeManager getInstance() { return INSTANCE; }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/homes.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) { e.printStackTrace(); }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (!dataConfig.contains("homes")) return;
        ConfigurationSection sec = dataConfig.getConfigurationSection("homes");
        if (sec == null) return;
        for (String uuid : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuid);
                Map<String, Location> playerHomes = new LinkedHashMap<>();
                ConfigurationSection ps = sec.getConfigurationSection(uuid);
                if (ps != null) {
                    for (String name : ps.getKeys(false)) {
                        Location loc = ps.getLocation(name);
                        if (loc != null) playerHomes.put(name, loc);
                    }
                }
                homes.put(id, playerHomes);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Location>> entry : homes.entrySet()) {
            for (Map.Entry<String, Location> home : entry.getValue().entrySet()) {
                dataConfig.set("homes." + entry.getKey().toString() + "." + home.getKey(), home.getValue());
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("[Hiru] Failed to save homes: " + e.getMessage());
        }
    }

    public boolean setHome(UUID uuid, String name, Location location) {
        Map<String, Location> playerHomes = homes.computeIfAbsent(uuid, k -> new LinkedHashMap<>());
        int limit = getHomeLimit(uuid);
        if (!playerHomes.containsKey(name) && playerHomes.size() >= limit) return false;
        playerHomes.put(name, location.clone());
        saveAll();
        return true;
    }

    public boolean deleteHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null || !playerHomes.containsKey(name)) return false;
        playerHomes.remove(name);
        saveAll();
        return true;
    }

    public Location getHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid);
        return playerHomes != null ? playerHomes.get(name) : null;
    }

    public Map<String, Location> getHomes(UUID uuid) {
        return Collections.unmodifiableMap(homes.getOrDefault(uuid, Collections.emptyMap()));
    }

    public int getHomeCount(UUID uuid) {
        return homes.getOrDefault(uuid, Collections.emptyMap()).size();
    }

    public int getHomeLimit(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return plugin.getConfigFile().getInt("homes.limits.default", 1);

        ConfigurationSection limits = plugin.getConfigFile().getConfigurationSection("homes.limits");
        if (limits == null) return 1;

        int max = limits.getInt("default", 1);
        for (String rank : limits.getKeys(false)) {
            if (rank.equals("default")) continue;
            if (player.hasPermission("hiru.homes." + rank)) {
                max = Math.max(max, limits.getInt(rank));
            }
        }
        return max;
    }

    public void shutdown() { saveAll(); }
}
