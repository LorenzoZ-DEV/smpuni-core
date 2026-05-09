package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Config;
import dev.lorenzz.solum.utils.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AFKManager {

    private static AFKManager INSTANCE;

    private final Solum plugin;
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final List<AFKZone> afkZones = new ArrayList<>();

    /** Tracks how many seconds each player has been in the AFK zone since their last reward */
    private final Map<UUID, Integer> afkZoneTimers = new ConcurrentHashMap<>();
    /** Active boss bars shown to players in AFK zones */
    private final Map<UUID, BossBar> afkZoneBossBars = new ConcurrentHashMap<>();
    /** Stores the player's previous location before being teleported to the AFK zone */
    private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();
    /** Players who are immune to movement detection (just teleported to AFK zone) */
    private final Map<UUID, Long> teleportImmunity = new ConcurrentHashMap<>();

    /** Dedicated file for AFK zone data (separate from config.yml to prevent resets) */
    private File afkZonesFile;
    private YamlConfiguration afkZonesConfig;

    private BukkitTask checkTask;
    private BukkitTask zoneTickTask;
    private BukkitTask backupTask;

    public AFKManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static AFKManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        loadAFKZones();

        int kickMinutes = plugin.getConfigFile().getInt("afk.kick-after-minutes", 30);
        boolean kickEnabled = plugin.getConfigFile().getBoolean("afk.kick-enabled", true);

        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                long last = lastActivity.getOrDefault(uuid, now);
                long idleMinutes = (now - last) / 60000;

                if (idleMinutes >= 5 && !afkPlayers.contains(uuid)) {
                    setAFK(player, true);
                }

                if (kickEnabled && idleMinutes >= kickMinutes) {
                    String exemptPerm = plugin.getConfigFile().getString("afk.exempt-permission", "solum.afk.exempt");
                    if (!player.hasPermission(exemptPerm) && !isInAFKZone(player)) {
                        String kickMsg = plugin.getConfigFile().getString("afk.kick-message", "&cKicked for inactivity!");
                        player.kick(Messages.colorComponent(kickMsg));
                    }
                }
            }
        }, 20L * 60, 20L * 60);

        // AFK Zone tick task - runs every second, handles bossbar/actionbar/rewards
        int rewardIntervalSeconds = plugin.getConfigFile().getInt("afk.zone-reward-interval-seconds", 60);
        int rewardShards = plugin.getConfigFile().getInt("afk.zone-reward-shards", 5);

        zoneTickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ShardManager sm = ShardManager.getInstance();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                boolean inZone = isInAFKZone(player);

                if (inZone) {
                    // Player is in AFK zone — increment timer
                    int elapsed = afkZoneTimers.getOrDefault(uuid, 0) + 1;

                    // Check if reward should be given
                    if (elapsed >= rewardIntervalSeconds && rewardShards > 0 && sm != null) {
                        sm.addShards(uuid, rewardShards);
                        String msg = plugin.getConfigFile().getString("afk.messages.zone-reward",
                                        "&7You received &d%amount% Shards &7for staying in the AFK Zone!")
                                .replace("%amount%", String.valueOf(rewardShards));
                        Messages.send(player, msg);
                        elapsed = 0; // Reset timer
                    }

                    afkZoneTimers.put(uuid, elapsed);

                    // Calculate remaining time
                    int remaining = rewardIntervalSeconds - elapsed;
                    String timeStr = formatTime(remaining);
                    float progress = Math.max(0f, Math.min(1f, (float) elapsed / rewardIntervalSeconds));

                    // Update or create BossBar
                    BossBar bossBar = afkZoneBossBars.get(uuid);
                    Component bossTitle = Messages.colorComponent(
                            "&#9C27B0&l⏳ AFK ZONE &#8C8C8C» &#E0E0E0Next reward in &#FFD700" + timeStr);
                    if (bossBar == null) {
                        bossBar = BossBar.bossBar(bossTitle, progress, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
                        afkZoneBossBars.put(uuid, bossBar);
                        player.showBossBar(bossBar);
                    } else {
                        bossBar.name(bossTitle);
                        bossBar.progress(progress);
                    }

                    // Send ActionBar
                    player.sendActionBar(Messages.colorComponent(
                            "&#9C27B0⏳ &#E0E0E0AFK Zone &#8C8C8C| &#FFD700" + timeStr +
                                    " &#8C8C8Cuntil &d" + rewardShards + " Shards"));

                } else {
                    // Player left AFK zone — clean up
                    if (afkZoneTimers.containsKey(uuid)) {
                        afkZoneTimers.remove(uuid);
                        BossBar bossBar = afkZoneBossBars.remove(uuid);
                        if (bossBar != null) {
                            player.hideBossBar(bossBar);
                        }
                    }
                }
            }
        }, 20L, 20L); // Every second

        // Hourly backup task for AFK zones
        backupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!afkZones.isEmpty()) {
                saveAFKZonesFile();
                createBackup();
                plugin.getLogger().info("[AFK] Hourly backup of AFK zones completed (" + afkZones.size() + " zone(s)).");
            }
        }, 20L * 60 * 60, 20L * 60 * 60); // Every hour
    }

    /**
     * Formats seconds into a readable time string (e.g. "4m 30s", "59s").
     */
    private String formatTime(int totalSeconds) {
        if (totalSeconds <= 0) return "0s";
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    private void loadAFKZones() {
        afkZones.clear();

        // Setup dedicated file
        afkZonesFile = new File(plugin.getDataFolder(), "afkzones.yml");
        afkZonesConfig = new YamlConfiguration();

        boolean fileExists = afkZonesFile.exists();

        // If the dedicated file exists, load from it
        if (fileExists) {
            try {
                afkZonesConfig.load(afkZonesFile);
            } catch (Exception ex) {
                plugin.getLogger().warning("[AFK] Failed to load afkzones.yml, attempting backup restore...");
                if (restoreFromBackup()) {
                    try {
                        afkZonesConfig.load(afkZonesFile);
                    } catch (Exception ex2) {
                        plugin.getLogger().severe("[AFK] Failed to load afkzones.yml even from backup: " + ex2.getMessage());
                    }
                }
            }
        }

        // Migrate from config.yml if afkzones.yml is empty/missing zones
        ConfigurationSection zonesSection = afkZonesConfig.getConfigurationSection("zones");
        if (zonesSection == null || zonesSection.getKeys(false).isEmpty()) {
            // Try to migrate from config.yml
            Config config = plugin.getConfigFile();
            ConfigurationSection oldZones = config.getConfigurationSection("afk.zones");
            if (oldZones != null && !oldZones.getKeys(false).isEmpty()) {
                plugin.getLogger().info("[AFK] Migrating AFK zones from config.yml to afkzones.yml...");
                for (String key : oldZones.getKeys(false)) {
                    String oldPath = "afk.zones." + key;
                    String newPath = "zones." + key;
                    afkZonesConfig.set(newPath + ".world", config.getString(oldPath + ".world", "world"));
                    afkZonesConfig.set(newPath + ".min-x", config.getInt(oldPath + ".min-x"));
                    afkZonesConfig.set(newPath + ".min-y", config.getInt(oldPath + ".min-y"));
                    afkZonesConfig.set(newPath + ".min-z", config.getInt(oldPath + ".min-z"));
                    afkZonesConfig.set(newPath + ".max-x", config.getInt(oldPath + ".max-x"));
                    afkZonesConfig.set(newPath + ".max-y", config.getInt(oldPath + ".max-y"));
                    afkZonesConfig.set(newPath + ".max-z", config.getInt(oldPath + ".max-z"));
                }
                saveAFKZonesFile();
                // Remove old zones from config.yml to avoid confusion
                config.set("afk.zones", null);
                config.saveFile();
                plugin.getLogger().info("[AFK] Migration complete. Old zones removed from config.yml.");
                zonesSection = afkZonesConfig.getConfigurationSection("zones");
            }
        }

        // Load zones into memory
        if (zonesSection != null) {
            for (String key : zonesSection.getKeys(false)) {
                String path = "zones." + key;
                String worldName = afkZonesConfig.getString(path + ".world", "world");
                int minX = afkZonesConfig.getInt(path + ".min-x");
                int minY = afkZonesConfig.getInt(path + ".min-y");
                int minZ = afkZonesConfig.getInt(path + ".min-z");
                int maxX = afkZonesConfig.getInt(path + ".max-x");
                int maxY = afkZonesConfig.getInt(path + ".max-y");
                int maxZ = afkZonesConfig.getInt(path + ".max-z");
                afkZones.add(new AFKZone(key, worldName, minX, minY, minZ, maxX, maxY, maxZ));
            }
        }

        plugin.getLogger().info("[AFK] Loaded " + afkZones.size() + " AFK zone(s).");

        // Create initial backup
        if (!afkZones.isEmpty()) {
            createBackup();
        }
    }

    /**
     * Saves the afkzones.yml file to disk.
     */
    private void saveAFKZonesFile() {
        try {
            afkZonesConfig.save(afkZonesFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("[AFK] Failed to save afkzones.yml: " + ex.getMessage());
        }
    }

    /**
     * Creates a backup of afkzones.yml.
     */
    private void createBackup() {
        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) backupDir.mkdirs();

            // Keep a rolling backup
            File backupFile = new File(backupDir, "afkzones_backup.yml");
            if (afkZonesFile.exists() && afkZonesFile.length() > 0) {
                Files.copy(afkZonesFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Also keep a timestamped backup (max 5)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            File timestampedBackup = new File(backupDir, "afkzones_" + timestamp + ".yml");
            if (afkZonesFile.exists() && afkZonesFile.length() > 0) {
                Files.copy(afkZonesFile.toPath(), timestampedBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // Clean old timestamped backups (keep last 5)
            File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("afkzones_") && name.endsWith(".yml") && !name.equals("afkzones_backup.yml"));
            if (backups != null && backups.length > 5) {
                Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < backups.length - 5; i++) {
                    backups[i].delete();
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("[AFK] Failed to create backup: " + ex.getMessage());
        }
    }

    /**
     * Attempts to restore afkzones.yml from the latest backup.
     * Returns true if restored successfully.
     */
    private boolean restoreFromBackup() {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        File backupFile = new File(backupDir, "afkzones_backup.yml");
        if (backupFile.exists() && backupFile.length() > 0) {
            try {
                Files.copy(backupFile.toPath(), afkZonesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("[AFK] Restored afkzones.yml from backup.");
                return true;
            } catch (IOException ex) {
                plugin.getLogger().severe("[AFK] Failed to restore from backup: " + ex.getMessage());
            }
        }
        return false;
    }

    public boolean isInAFKZone(Player player) {
        Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
        for (AFKZone zone : afkZones) {
            if (zone.contains(worldName, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets an AFK zone around the given location with the specified radius.
     * Saves it to afkzones.yml.
     */
    public void setAFKZone(String name, Location location, int radius) {
        int minX = location.getBlockX() - radius;
        int minY = Math.max(location.getBlockY() - radius, -64);
        int minZ = location.getBlockZ() - radius;
        int maxX = location.getBlockX() + radius;
        int maxY = Math.min(location.getBlockY() + radius, 320);
        int maxZ = location.getBlockZ() + radius;
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "world";

        AFKZone zone = new AFKZone(name, worldName, minX, minY, minZ, maxX, maxY, maxZ);
        // Remove existing zone with same name
        afkZones.removeIf(z -> z.name().equalsIgnoreCase(name));
        afkZones.add(zone);

        // Save to dedicated file
        String path = "zones." + name;
        afkZonesConfig.set(path + ".world", worldName);
        afkZonesConfig.set(path + ".min-x", minX);
        afkZonesConfig.set(path + ".min-y", minY);
        afkZonesConfig.set(path + ".min-z", minZ);
        afkZonesConfig.set(path + ".max-x", maxX);
        afkZonesConfig.set(path + ".max-y", maxY);
        afkZonesConfig.set(path + ".max-z", maxZ);
        saveAFKZonesFile();
        createBackup();
    }

    /**
     * Removes an AFK zone by name.
     */
    public boolean removeAFKZone(String name) {
        boolean removed = afkZones.removeIf(z -> z.name().equalsIgnoreCase(name));
        if (removed) {
            afkZonesConfig.set("zones." + name, null);
            saveAFKZonesFile();
            createBackup();
        }
        return removed;
    }

    /**
     * Returns all AFK zone names.
     */
    public List<String> getAFKZoneNames() {
        return afkZones.stream().map(AFKZone::name).toList();
    }

    /**
     * Returns the center Location of the first AFK zone, or null if no zones exist.
     */
    public Location getAFKZoneCenter() {
        if (afkZones.isEmpty()) return null;
        AFKZone zone = afkZones.get(0);
        var world = Bukkit.getWorld(zone.world());
        if (world == null) return null;
        double cx = (zone.minX() + zone.maxX()) / 2.0 + 0.5;
        double cy = (zone.minY() + zone.maxY()) / 2.0;
        double cz = (zone.minZ() + zone.maxZ()) / 2.0 + 0.5;
        return new Location(world, cx, cy, cz);
    }

    /**
     * Teleports the player to the AFK zone, saving their current location for later return.
     * Returns true if successful, false if no AFK zone is set.
     */
    public boolean teleportToAFKZone(Player player) {
        Location center = getAFKZoneCenter();
        if (center == null) return false;
        previousLocations.put(player.getUniqueId(), player.getLocation().clone());
        teleportImmunity.put(player.getUniqueId(), System.currentTimeMillis() + 3000L); // 3 second immunity
        player.teleportAsync(center);
        return true;
    }

    /**
     * Returns the player's saved previous location (before AFK zone teleport), or null.
     */
    public Location getPreviousLocation(UUID uuid) {
        return previousLocations.get(uuid);
    }

    /**
     * Clears and returns the previous location for the given player.
     */
    public Location clearPreviousLocation(UUID uuid) {
        return previousLocations.remove(uuid);
    }

    public void recordActivity(UUID uuid) {
        // Skip if player has teleport immunity (just teleported to AFK zone)
        Long immuneUntil = teleportImmunity.get(uuid);
        if (immuneUntil != null) {
            if (System.currentTimeMillis() < immuneUntil) {
                return; // Still immune, ignore movement
            } else {
                teleportImmunity.remove(uuid);
            }
        }

        lastActivity.put(uuid, System.currentTimeMillis());
        if (afkPlayers.contains(uuid)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) setAFK(player, false);
        }
    }

    public void toggleAFK(Player player) {
        setAFK(player, !isAFK(player));
    }

    public void setAFK(Player player, boolean afk) {
        UUID uuid = player.getUniqueId();
        if (afk) {
            afkPlayers.add(uuid);
            String msg = plugin.getConfigFile().getString("afk.messages.now-afk", "&7%player% &7is now &eAFK&7.")
                    .replace("%player%", player.getName());
            Bukkit.broadcast(Messages.colorComponent(Messages.getPrefix() + msg));
        } else {
            afkPlayers.remove(uuid);
            lastActivity.put(uuid, System.currentTimeMillis());

            // Teleport back to previous location if they were in the AFK zone
            Location prevLoc = clearPreviousLocation(uuid);
            if (prevLoc != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleportAsync(prevLoc);
                    }
                });
            }

            String msg = plugin.getConfigFile().getString("afk.messages.no-longer-afk", "&7%player% &7is no longer &eAFK&7.")
                    .replace("%player%", player.getName());
            Bukkit.broadcast(Messages.colorComponent(Messages.getPrefix() + msg));
        }
    }

    public boolean isAFK(Player player) { return afkPlayers.contains(player.getUniqueId()); }
    public boolean isAFK(UUID uuid) { return afkPlayers.contains(uuid); }

    public void playerJoin(UUID uuid) { lastActivity.put(uuid, System.currentTimeMillis()); }

    public void playerQuit(UUID uuid) {
        afkPlayers.remove(uuid);
        lastActivity.remove(uuid);
        afkZoneTimers.remove(uuid);
        previousLocations.remove(uuid);
        teleportImmunity.remove(uuid);
        BossBar bar = afkZoneBossBars.remove(uuid);
        if (bar != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.hideBossBar(bar);
        }
    }

    public void shutdown() {
        if (checkTask != null) checkTask.cancel();
        if (zoneTickTask != null) zoneTickTask.cancel();
        if (backupTask != null) backupTask.cancel();

        // Save AFK zones and create backup on shutdown
        if (!afkZones.isEmpty()) {
            saveAFKZonesFile();
            createBackup();
            plugin.getLogger().info("[AFK] AFK zones saved on shutdown (" + afkZones.size() + " zone(s)).");
        }

        // Clean up all boss bars
        for (Map.Entry<UUID, BossBar> entry : afkZoneBossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) player.hideBossBar(entry.getValue());
        }
        afkZoneBossBars.clear();
        afkZoneTimers.clear();
        previousLocations.clear();
        teleportImmunity.clear();
        afkPlayers.clear();
        lastActivity.clear();
    }

    public record AFKZone(String name, String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean contains(String worldName, int x, int y, int z) {
            return worldName.equals(world)
                    && x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }
}
