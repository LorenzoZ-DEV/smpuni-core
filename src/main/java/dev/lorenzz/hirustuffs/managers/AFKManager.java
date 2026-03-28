package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Config;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AFKManager {

    private static AFKManager INSTANCE;

    private final Hiru plugin;
    private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final List<AFKZone> afkZones = new ArrayList<>();

    /** Tracks how many seconds each player has been in the AFK zone since their last reward */
    private final Map<UUID, Integer> afkZoneTimers = new ConcurrentHashMap<>();
    /** Active boss bars shown to players in AFK zones */
    private final Map<UUID, BossBar> afkZoneBossBars = new ConcurrentHashMap<>();

    private BukkitTask checkTask;
    private BukkitTask zoneTickTask;

    public AFKManager(Hiru plugin) {
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
                    String exemptPerm = plugin.getConfigFile().getString("afk.exempt-permission", "hiru.afk.exempt");
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
                                    " &#8C8C8Cuntil &#d" + rewardShards + " Shards"));

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
        Config config = plugin.getConfigFile();
        ConfigurationSection zones = config.getConfigurationSection("afk.zones");
        if (zones == null) return;

        for (String key : zones.getKeys(false)) {
            String path = "afk.zones." + key;
            String worldName = config.getString(path + ".world", "world");
            int minX = config.getInt(path + ".min-x");
            int minY = config.getInt(path + ".min-y");
            int minZ = config.getInt(path + ".min-z");
            int maxX = config.getInt(path + ".max-x");
            int maxY = config.getInt(path + ".max-y");
            int maxZ = config.getInt(path + ".max-z");
            afkZones.add(new AFKZone(key, worldName, minX, minY, minZ, maxX, maxY, maxZ));
        }
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
     * Saves it to config.yml under afk.zones.
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

        // Save to config
        Config config = plugin.getConfigFile();
        String path = "afk.zones." + name;
        config.set(path + ".world", worldName);
        config.set(path + ".min-x", minX);
        config.set(path + ".min-y", minY);
        config.set(path + ".min-z", minZ);
        config.set(path + ".max-x", maxX);
        config.set(path + ".max-y", maxY);
        config.set(path + ".max-z", maxZ);
        config.saveFile();
    }

    /**
     * Removes an AFK zone by name.
     */
    public boolean removeAFKZone(String name) {
        boolean removed = afkZones.removeIf(z -> z.name().equalsIgnoreCase(name));
        if (removed) {
            Config config = plugin.getConfigFile();
            config.set("afk.zones." + name, null);
            config.saveFile();
        }
        return removed;
    }

    /**
     * Returns all AFK zone names.
     */
    public List<String> getAFKZoneNames() {
        return afkZones.stream().map(AFKZone::name).toList();
    }

    public void recordActivity(UUID uuid) {
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
        BossBar bar = afkZoneBossBars.remove(uuid);
        if (bar != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.hideBossBar(bar);
        }
    }

    public void shutdown() {
        if (checkTask != null) checkTask.cancel();
        if (zoneTickTask != null) zoneTickTask.cancel();
        // Clean up all boss bars
        for (Map.Entry<UUID, BossBar> entry : afkZoneBossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) player.hideBossBar(entry.getValue());
        }
        afkZoneBossBars.clear();
        afkZoneTimers.clear();
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
