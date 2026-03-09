package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SpawnManager {

    private static SpawnManager INSTANCE;

    private final Hiru plugin;
    private final Map<String, Location> spawns = new HashMap<>();

    public SpawnManager(Hiru plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static SpawnManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        loadSpawns();
    }

    private void loadSpawns() {
        spawns.clear();
        Config config = plugin.getConfigFile();
        if (!config.contains("spawn.spawns")) return;

        var section = config.getConfigurationSection("spawn.spawns");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            String path = "spawn.spawns." + name;
            String worldName = config.getString(path + ".world", "world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            float yaw = (float) config.getDouble(path + ".yaw");
            float pitch = (float) config.getDouble(path + ".pitch");
            spawns.put(name.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
        }
    }

    public void setSpawn(String name, Location location) {
        spawns.put(name.toLowerCase(), location);
        Config config = plugin.getConfigFile();
        String path = "spawn.spawns." + name.toLowerCase();
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        config.saveFile();
    }

    public Location getSpawn(String name) {
        return spawns.get(name.toLowerCase());
    }

    public Location getDefaultSpawn() {
        if (spawns.containsKey("default")) return spawns.get("default");
        if (!spawns.isEmpty()) return spawns.values().iterator().next();
        return null;
    }

    public Set<String> getSpawnNames() {
        return spawns.keySet();
    }

    public boolean hasSpawn(String name) {
        return spawns.containsKey(name.toLowerCase());
    }

    public void shutdown() {
        spawns.clear();
    }
}
