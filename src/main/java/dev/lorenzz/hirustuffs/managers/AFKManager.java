package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Config;
import dev.lorenzz.hirustuffs.utils.Messages;
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
    private BukkitTask checkTask;

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
                        String kickMsg = plugin.getConfigFile().getString("afk.kick-message", "&cKickato per inattività!");
                        player.kick(Messages.colorComponent(kickMsg));
                    }
                }
            }
        }, 20L * 60, 20L * 60);
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
            afkZones.add(new AFKZone(worldName, minX, minY, minZ, maxX, maxY, maxZ));
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
            String msg = plugin.getConfigFile().getString("afk.messages.now-afk", "&7%player% è AFK.")
                    .replace("%player%", player.getName());
            Bukkit.broadcast(Messages.colorComponent(Messages.getPrefix() + msg));
        } else {
            afkPlayers.remove(uuid);
            lastActivity.put(uuid, System.currentTimeMillis());
            String msg = plugin.getConfigFile().getString("afk.messages.no-longer-afk", "&7%player% non è più AFK.")
                    .replace("%player%", player.getName());
            Bukkit.broadcast(Messages.colorComponent(Messages.getPrefix() + msg));
        }
    }

    public boolean isAFK(Player player) { return afkPlayers.contains(player.getUniqueId()); }
    public boolean isAFK(UUID uuid) { return afkPlayers.contains(uuid); }

    public void playerJoin(UUID uuid) { lastActivity.put(uuid, System.currentTimeMillis()); }
    public void playerQuit(UUID uuid) { afkPlayers.remove(uuid); lastActivity.remove(uuid); }

    public void shutdown() {
        if (checkTask != null) checkTask.cancel();
        afkPlayers.clear();
        lastActivity.clear();
    }

    private record AFKZone(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean contains(String worldName, int x, int y, int z) {
            return worldName.equals(world)
                    && x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }
    }
}
