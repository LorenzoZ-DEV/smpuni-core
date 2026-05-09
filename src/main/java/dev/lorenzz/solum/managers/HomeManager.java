package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class HomeManager {

    private static HomeManager INSTANCE;

    private final Solum plugin;
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();
    /**
     * Stores raw home data for homes whose world was not yet loaded.
     * Key: UUID, Value: Map of homeName -> raw config data (world, x, y, z, yaw, pitch).
     * These are preserved across saves so data is never lost.
     */
    private final Map<UUID, Map<String, Map<String, Object>>> pendingHomes = new ConcurrentHashMap<>();
    private File dataFile;
    private File backupDir;
    private static final int MAX_BACKUPS = 24; // Keep last 24 hourly backups
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private BukkitTask autoSaveTask;
    private BukkitTask backupTask;

    public HomeManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static HomeManager getInstance() { return INSTANCE; }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/homes.yml");
        backupDir = new File(plugin.getDataFolder(), "data/backups/homes");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            Files.createDirectories(backupDir.toPath());
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Solum] Failed to create homes data file", e);
        }
        // Safety: create a pre-load backup of the raw file before touching anything
        createPreLoadBackup();

        loadAll();

        // If homes file is empty/corrupted but backups exist, restore from latest valid backup
        if (homes.isEmpty() && pendingHomes.isEmpty()) {
            restoreFromBackupIfAvailable();
        }

        // Create an immediate backup after successful load so we always have a recent copy
        if (!homes.isEmpty() || !pendingHomes.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, this::createBackup);
        }

        startAutoSave();
        startBackupTask();
        // Retry resolving pending homes after all worlds are loaded (5 seconds = 100 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, this::resolvePendingHomes, 100L);
    }

    private void loadAll() {
        homes.clear();
        pendingHomes.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (!config.contains("homes")) return;
        ConfigurationSection sec = config.getConfigurationSection("homes");
        if (sec == null) return;
        for (String uuid : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuid);
                Map<String, Location> playerHomes = new LinkedHashMap<>();
                Map<String, Map<String, Object>> playerPending = new LinkedHashMap<>();
                ConfigurationSection ps = sec.getConfigurationSection(uuid);
                if (ps != null) {
                    for (String name : ps.getKeys(false)) {
                        ConfigurationSection homeSec = ps.getConfigurationSection(name);
                        if (homeSec == null) continue;
                        String worldName = homeSec.getString("world");
                        if (worldName == null) continue;
                        double x = homeSec.getDouble("x");
                        double y = homeSec.getDouble("y");
                        double z = homeSec.getDouble("z");
                        float yaw = (float) homeSec.getDouble("yaw");
                        float pitch = (float) homeSec.getDouble("pitch");

                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            // World not loaded yet — store raw data so it's not lost
                            Map<String, Object> raw = new LinkedHashMap<>();
                            raw.put("world", worldName);
                            raw.put("x", x);
                            raw.put("y", y);
                            raw.put("z", z);
                            raw.put("yaw", (double) yaw);
                            raw.put("pitch", (double) pitch);
                            playerPending.put(name, raw);
                            continue;
                        }
                        playerHomes.put(name, new Location(world, x, y, z, yaw, pitch));
                    }
                }
                if (!playerHomes.isEmpty()) {
                    homes.put(id, playerHomes);
                }
                if (!playerPending.isEmpty()) {
                    pendingHomes.put(id, playerPending);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        int pendingCount = pendingHomes.values().stream().mapToInt(Map::size).sum();
        plugin.getLogger().info("[Solum] Loaded " + homes.size() + " player home sets." +
                (pendingCount > 0 ? " (" + pendingCount + " homes pending world load)" : ""));
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
     * Attempts to resolve pending homes whose worlds were not loaded at startup.
     */
    private void resolvePendingHomes() {
        if (pendingHomes.isEmpty()) return;
        int resolved = 0;
        Iterator<Map.Entry<UUID, Map<String, Map<String, Object>>>> it = pendingHomes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Map<String, Map<String, Object>>> entry = it.next();
            UUID uuid = entry.getKey();
            Map<String, Map<String, Object>> pending = entry.getValue();
            Iterator<Map.Entry<String, Map<String, Object>>> homeIt = pending.entrySet().iterator();
            while (homeIt.hasNext()) {
                Map.Entry<String, Map<String, Object>> homeEntry = homeIt.next();
                Map<String, Object> raw = homeEntry.getValue();
                String worldName = (String) raw.get("world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = (double) raw.get("x");
                    double y = (double) raw.get("y");
                    double z = (double) raw.get("z");
                    float yaw = ((Double) raw.get("yaw")).floatValue();
                    float pitch = ((Double) raw.get("pitch")).floatValue();
                    Location loc = new Location(world, x, y, z, yaw, pitch);
                    homes.computeIfAbsent(uuid, k -> Collections.synchronizedMap(new LinkedHashMap<>()))
                            .put(homeEntry.getKey(), loc);
                    homeIt.remove();
                    resolved++;
                }
            }
            if (pending.isEmpty()) it.remove();
        }
        if (resolved > 0) {
            plugin.getLogger().info("[Solum] Resolved " + resolved + " pending homes after world load.");
            markDirty();
        }
        if (!pendingHomes.isEmpty()) {
            int remaining = pendingHomes.values().stream().mapToInt(Map::size).sum();
            plugin.getLogger().warning("[Solum] " + remaining + " homes still have unresolved worlds. They will be preserved in the save file.");
        }
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

        // Also save pending homes (worlds that were never resolved) to avoid data loss
        for (Map.Entry<UUID, Map<String, Map<String, Object>>> entry : pendingHomes.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Map<String, Object>> homeEntry : entry.getValue().entrySet()) {
                String path = "homes." + uuidStr + "." + homeEntry.getKey();
                Map<String, Object> raw = homeEntry.getValue();
                config.set(path + ".world", raw.get("world"));
                config.set(path + ".x", raw.get("x"));
                config.set(path + ".y", raw.get("y"));
                config.set(path + ".z", raw.get("z"));
                config.set(path + ".yaw", raw.get("yaw"));
                config.set(path + ".pitch", raw.get("pitch"));
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Solum] Failed to save homes!", e);
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
            if (player.hasPermission("solum.homes." + rank)) {
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
        if (backupTask != null) {
            backupTask.cancel();
            backupTask = null;
        }
        saveAllSync();
        // Create a final backup on shutdown for safety
        createBackup();
    }

    // ========================= BACKUP SYSTEM =========================

    /**
     * Creates a safety backup of the raw homes.yml before loading/processing.
     * This ensures that even if loading corrupts or loses data, we have the original file.
     */
    private void createPreLoadBackup() {
        try {
            if (!dataFile.exists() || dataFile.length() == 0) return;

            YamlConfiguration verify = YamlConfiguration.loadConfiguration(dataFile);
            if (!verify.contains("homes")) return;
            int homeCount = countHomesInConfig(verify);
            if (homeCount == 0) return;

            File preLoadBackup = new File(backupDir, "homes_preload.yml");
            Files.copy(dataFile.toPath(), preLoadBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("[Solum] Pre-load homes backup created (" + homeCount + " homes)");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Solum] Failed to create pre-load homes backup!", e);
        }
    }

    /**
     * Starts a task that creates a backup of homes.yml every hour (72000 ticks).
     * Only creates a backup if there is actual data to back up.
     */
    private void startBackupTask() {
        backupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Only back up if we have data worth saving
            if (!homes.isEmpty() || !pendingHomes.isEmpty()) {
                createBackup();
            }
        }, 72000L, 72000L); // 72000 ticks = 1 hour
    }

    /**
     * Creates a timestamped backup of homes.yml.
     */
    private synchronized void createBackup() {
        try {
            if (!dataFile.exists() || dataFile.length() == 0) return;

            // Verify the file has actual home data before backing up
            YamlConfiguration verify = YamlConfiguration.loadConfiguration(dataFile);
            if (!verify.contains("homes") || verify.getConfigurationSection("homes") == null) return;
            int homeCount = countHomesInConfig(verify);
            if (homeCount == 0) return;

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File backupFile = new File(backupDir, "homes_" + timestamp + ".yml");
            Files.copy(dataFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            plugin.getLogger().info("[Solum] Homes backup created: " + backupFile.getName() + " (" + homeCount + " homes)");

            cleanOldBackups();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[Solum] Failed to create homes backup!", e);
        }
    }

    /**
     * Tries to restore homes from the latest valid backup file.
     * Called when the main homes.yml is empty/corrupted on startup.
     */
    private void restoreFromBackupIfAvailable() {
        if (!backupDir.exists()) return;

        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("homes_") && name.endsWith(".yml"));
        if (backups == null || backups.length == 0) return;

        // Sort by last modified descending (newest first)
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());

        for (File backup : backups) {
            try {
                YamlConfiguration backupConfig = YamlConfiguration.loadConfiguration(backup);
                if (!backupConfig.contains("homes")) continue;
                int homeCount = countHomesInConfig(backupConfig);
                if (homeCount == 0) continue;

                // Valid backup found — restore it
                plugin.getLogger().warning("[Solum] homes.yml is empty/corrupted! Restoring from backup: " + backup.getName() + " (" + homeCount + " homes)");
                Files.copy(backup.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // Reload the data from the restored file
                loadAll();
                plugin.getLogger().info("[Solum] Homes restored successfully from backup!");
                return;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "[Solum] Invalid backup, trying next: " + backup.getName(), e);
            }
        }

        plugin.getLogger().severe("[Solum] No valid backup found to restore homes!");
    }

    /**
     * Removes old backups keeping only the latest MAX_BACKUPS files.
     */
    private void cleanOldBackups() {
        try {
            File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("homes_") && name.endsWith(".yml"));
            if (backups == null || backups.length <= MAX_BACKUPS) return;

            // Sort by last modified ascending (oldest first)
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

            int toDelete = backups.length - MAX_BACKUPS;
            for (int i = 0; i < toDelete; i++) {
                if (backups[i].delete()) {
                    plugin.getLogger().info("[Solum] Old backup removed: " + backups[i].getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Solum] Error while cleaning up old backups", e);
        }
    }

    /**
     * Counts the total number of individual homes in a YamlConfiguration.
     */
    private int countHomesInConfig(YamlConfiguration config) {
        ConfigurationSection sec = config.getConfigurationSection("homes");
        if (sec == null) return 0;
        int count = 0;
        for (String uuid : sec.getKeys(false)) {
            ConfigurationSection ps = sec.getConfigurationSection(uuid);
            if (ps != null) {
                count += ps.getKeys(false).size();
            }
        }
        return count;
    }
}
