package dev.lorenzz.lxsmp.managers;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarpManager {

    private static WarpManager INSTANCE;

    private final LXSMP plugin;
    private final Map<String, Location> warps = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> pendingWarps = new LinkedHashMap<>();
    private File warpsFile;
    private YamlConfiguration warpsConfig;

    public WarpManager(LXSMP plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static WarpManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                warpsFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                warpsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create warps.yml: " + e.getMessage());
            }
        }
        warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        loadWarps();

        // Retry resolving pending warps after all worlds are loaded
        Bukkit.getScheduler().runTaskLater(plugin, this::resolvePendingWarps, 100L);
    }

    private void loadWarps() {
        warps.clear();
        pendingWarps.clear();

        for (String name : warpsConfig.getKeys(false)) {
            String worldName = warpsConfig.getString(name + ".world", "world");
            double x = warpsConfig.getDouble(name + ".x");
            double y = warpsConfig.getDouble(name + ".y");
            double z = warpsConfig.getDouble(name + ".z");
            float yaw = (float) warpsConfig.getDouble(name + ".yaw");
            float pitch = (float) warpsConfig.getDouble(name + ".pitch");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                Map<String, Object> raw = new LinkedHashMap<>();
                raw.put("world", worldName);
                raw.put("x", x);
                raw.put("y", y);
                raw.put("z", z);
                raw.put("yaw", (double) yaw);
                raw.put("pitch", (double) pitch);
                pendingWarps.put(name.toLowerCase(), raw);
                continue;
            }

            warps.put(name.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
        }
        plugin.getLogger().info("[Solum] Loaded " + warps.size() + " warp(s)." +
                (!pendingWarps.isEmpty() ? " (" + pendingWarps.size() + " pending world load)" : ""));
    }

    private void resolvePendingWarps() {
        if (pendingWarps.isEmpty()) return;
        int resolved = 0;
        var it = pendingWarps.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Map<String, Object> raw = entry.getValue();
            String worldName = (String) raw.get("world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = (double) raw.get("x");
                double y = (double) raw.get("y");
                double z = (double) raw.get("z");
                float yaw = ((Double) raw.get("yaw")).floatValue();
                float pitch = ((Double) raw.get("pitch")).floatValue();
                warps.put(entry.getKey(), new Location(world, x, y, z, yaw, pitch));
                it.remove();
                resolved++;
            }
        }
        if (resolved > 0) {
            plugin.getLogger().info("[Solum] Resolved " + resolved + " pending warp(s).");
        }
    }

    public void setWarp(String name, Location location) {
        warps.put(name.toLowerCase(), location.clone());
        pendingWarps.remove(name.toLowerCase());
        String path = name.toLowerCase();
        warpsConfig.set(path + ".world", location.getWorld().getName());
        warpsConfig.set(path + ".x", location.getX());
        warpsConfig.set(path + ".y", location.getY());
        warpsConfig.set(path + ".z", location.getZ());
        warpsConfig.set(path + ".yaw", location.getYaw());
        warpsConfig.set(path + ".pitch", location.getPitch());
        saveWarpsFile();
    }

    public boolean deleteWarp(String name) {
        Location removed = warps.remove(name.toLowerCase());
        pendingWarps.remove(name.toLowerCase());
        if (removed != null || warpsConfig.contains(name.toLowerCase())) {
            warpsConfig.set(name.toLowerCase(), null);
            saveWarpsFile();
            return true;
        }
        return false;
    }

    public Location getWarp(String name) {
        return warps.get(name.toLowerCase());
    }

    public Set<String> getWarpNames() {
        return Collections.unmodifiableSet(warps.keySet());
    }

    public boolean hasWarp(String name) {
        return warps.containsKey(name.toLowerCase());
    }

    private void saveWarpsFile() {
        for (Map.Entry<String, Map<String, Object>> entry : pendingWarps.entrySet()) {
            String path = entry.getKey();
            Map<String, Object> raw = entry.getValue();
            warpsConfig.set(path + ".world", raw.get("world"));
            warpsConfig.set(path + ".x", raw.get("x"));
            warpsConfig.set(path + ".y", raw.get("y"));
            warpsConfig.set(path + ".z", raw.get("z"));
            warpsConfig.set(path + ".yaw", raw.get("yaw"));
            warpsConfig.set(path + ".pitch", raw.get("pitch"));
        }
        try {
            warpsConfig.save(warpsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save warps.yml: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (warpsConfig != null) {
            saveWarpsFile();
        }
        warps.clear();
    }
}
