package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class HomeManager {

    private static HomeManager INSTANCE;

    private final Hiru plugin;
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();
    private File dataFile;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private BukkitTask autoSaveTask;

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
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Hiru] Failed to create homes data file", e);
        }
        loadAll();
        startAutoSave();
    }

    private void loadAll() {
        homes.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (!config.contains("homes")) return;
        ConfigurationSection sec = config.getConfigurationSection("homes");
        if (sec == null) return;
        for (String uuid : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuid);
                Map<String, Location> playerHomes = new LinkedHashMap<>();
                ConfigurationSection ps = sec.getConfigurationSection(uuid);
                if (ps != null) {
                    for (String name : ps.getKeys(false)) {
                        ConfigurationSection homeSec = ps.getConfigurationSection(name);
                        if (homeSec == null) continue;
                        String worldName = homeSec.getString("world");
                        if (worldName == null) continue;
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) continue;
                        double x = homeSec.getDouble("x");
                        double y = homeSec.getDouble("y");
                        double z = homeSec.getDouble("z");
                        float yaw = (float) homeSec.getDouble("yaw");
                        float pitch = (float) homeSec.getDouble("pitch");
                        playerHomes.put(name, new Location(world, x, y, z, yaw, pitch));
                    }
                }
                if (!playerHomes.isEmpty()) {
                    homes.put(id, playerHomes);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        plugin.getLogger().info("[Hiru] Loaded " + homes.size() + " player home sets.");
    }

    private void startAutoSave() {
        // Auto-save every 60 seconds (1200 ticks) if dirty
        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (dirty.compareAndSet(true, false)) {
                saveAllSync();
            }
        }, 1200L, 1200L);
    }

    private void markDirty() {
        dirty.set(true);
    }

    /**
     * Saves all homes to disk. Thread-safe: builds YAML in memory then writes.
     */
    private synchronized void saveAllSync() {
        YamlConfiguration config = new YamlConfiguration();
        // Snapshot the data to avoid concurrent modification
        Map<UUID, Map<String, Location>> snapshot = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Location>> entry : homes.entrySet()) {
            synchronized (entry.getValue()) {
                snapshot.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }
        }

        for (Map.Entry<UUID, Map<String, Location>> entry : snapshot.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Location> home : entry.getValue().entrySet()) {
                String path = "homes." + uuidStr + "." + home.getKey();
                Location loc = home.getValue();
                config.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
                config.set(path + ".x", loc.getX());
                config.set(path + ".y", loc.getY());
                config.set(path + ".z", loc.getZ());
                config.set(path + ".yaw", loc.getYaw());
                config.set(path + ".pitch", loc.getPitch());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Hiru] Failed to save homes!", e);
            dirty.set(true); // Retry next cycle
        }
    }

    /**
     * Immediately saves to disk (blocking). Use for shutdown.
     */
    public void saveAll() {
        saveAllSync();
    }

    public boolean setHome(UUID uuid, String name, Location location) {
        Map<String, Location> playerHomes = homes.computeIfAbsent(uuid, k -> Collections.synchronizedMap(new LinkedHashMap<>()));
        synchronized (playerHomes) {
            int limit = getHomeLimit(uuid);
            if (!playerHomes.containsKey(name) && playerHomes.size() >= limit) return false;
            playerHomes.put(name, location.clone());
        }
        markDirty();
        // Schedule an immediate async save for reliability
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveAllSync);
        return true;
    }

    public boolean deleteHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) return false;
        boolean removed;
        synchronized (playerHomes) {
            removed = playerHomes.remove(name) != null;
        }
        if (removed) {
            markDirty();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveAllSync);
        }
        return removed;
    }

    public Location getHome(UUID uuid, String name) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) return null;
        synchronized (playerHomes) {
            Location loc = playerHomes.get(name);
            return loc != null ? loc.clone() : null;
        }
    }

    public Map<String, Location> getHomes(UUID uuid) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) return Collections.emptyMap();
        synchronized (playerHomes) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(playerHomes));
        }
    }

    public int getHomeCount(UUID uuid) {
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null) return 0;
        synchronized (playerHomes) {
            return playerHomes.size();
        }
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

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        saveAllSync();
    }
}
