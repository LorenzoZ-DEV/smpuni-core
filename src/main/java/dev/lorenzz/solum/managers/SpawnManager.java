package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class SpawnManager {

    private static SpawnManager INSTANCE;

    private final Solum plugin;
    private final Map<String, Location> spawns = new HashMap<>();
    /**
     * Raw spawn data for spawns whose world was not loaded at startup.
     * Preserved across saves so data is never lost.
     */
    private final Map<String, Map<String, Object>> pendingSpawns = new LinkedHashMap<>();
    private File spawnsFile;
    private YamlConfiguration spawnsConfig;

    public SpawnManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static SpawnManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        spawnsFile = new File(plugin.getDataFolder(), "spawns.yml");
        if (!spawnsFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                spawnsFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                spawnsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create spawns.yml: " + e.getMessage());
            }
        }
        spawnsConfig = YamlConfiguration.loadConfiguration(spawnsFile);
        migrateFromConfig();
        loadSpawns();

        // Retry resolving pending spawns after all worlds are loaded (5 seconds = 100 ticks)
        Bukkit.getScheduler().runTaskLater(plugin, this::resolvePendingSpawns, 100L);
    }

    /**
     * Migra gli spawn dal config.yml principale al file spawns.yml dedicato
     * (eseguito solo una volta, se ci sono spawn nel vecchio formato)
     */
    private void migrateFromConfig() {
        var mainConfig = plugin.getConfigFile();
        ConfigurationSection oldSection = mainConfig.getConfigurationSection("spawn.spawns");
        if (oldSection == null || oldSection.getKeys(false).isEmpty()) return;

        // If spawns.yml is already populated, don't migrate
        if (!spawnsConfig.getKeys(false).isEmpty()) return;

        plugin.getLogger().info("[Solum] Migrating spawns from config.yml to spawns.yml...");
        for (String name : oldSection.getKeys(false)) {
            String oldPath = "spawn.spawns." + name;
            spawnsConfig.set(name + ".world", mainConfig.getString(oldPath + ".world"));
            spawnsConfig.set(name + ".x", mainConfig.getDouble(oldPath + ".x"));
            spawnsConfig.set(name + ".y", mainConfig.getDouble(oldPath + ".y"));
            spawnsConfig.set(name + ".z", mainConfig.getDouble(oldPath + ".z"));
            spawnsConfig.set(name + ".yaw", mainConfig.getDouble(oldPath + ".yaw"));
            spawnsConfig.set(name + ".pitch", mainConfig.getDouble(oldPath + ".pitch"));
        }
        saveSpawnsFile();

        // Remove old spawns from config.yml for cleanup
        mainConfig.set("spawn.spawns", null);
        mainConfig.saveFile();
        plugin.getLogger().info("[Solum] Spawn migration complete.");
    }

    private void loadSpawns() {
        spawns.clear();
        pendingSpawns.clear();

        for (String name : spawnsConfig.getKeys(false)) {
            String worldName = spawnsConfig.getString(name + ".world", "world");
            double x = spawnsConfig.getDouble(name + ".x");
            double y = spawnsConfig.getDouble(name + ".y");
            double z = spawnsConfig.getDouble(name + ".z");
            float yaw = (float) spawnsConfig.getDouble(name + ".yaw");
            float pitch = (float) spawnsConfig.getDouble(name + ".pitch");

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                // World not loaded yet — store raw data so it's not lost
                plugin.getLogger().warning("[Solum] Spawn '" + name + "': world '" + worldName + "' not loaded yet, queued as pending.");
                Map<String, Object> raw = new LinkedHashMap<>();
                raw.put("world", worldName);
                raw.put("x", x);
                raw.put("y", y);
                raw.put("z", z);
                raw.put("yaw", (double) yaw);
                raw.put("pitch", (double) pitch);
                pendingSpawns.put(name.toLowerCase(), raw);
                continue;
            }

            spawns.put(name.toLowerCase(), new Location(world, x, y, z, yaw, pitch));
        }
        plugin.getLogger().info("[Solum] Loaded " + spawns.size() + " spawn(s)." +
                (!pendingSpawns.isEmpty() ? " (" + pendingSpawns.size() + " pending world load)" : ""));
    }

    /**
     * Attempts to resolve pending spawns whose worlds were not loaded at startup.
     */
    private void resolvePendingSpawns() {
        if (pendingSpawns.isEmpty()) return;
        int resolved = 0;
        var it = pendingSpawns.entrySet().iterator();
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
                spawns.put(entry.getKey(), new Location(world, x, y, z, yaw, pitch));
                it.remove();
                resolved++;
            }
        }
        if (resolved > 0) {
            plugin.getLogger().info("[Solum] Resolved " + resolved + " pending spawn(s) after world load.");
        }
        if (!pendingSpawns.isEmpty()) {
            plugin.getLogger().warning("[Solum] " + pendingSpawns.size() + " spawn(s) still have unresolved worlds. They will be preserved in saves.");
        }
    }

    public void setSpawn(String name, Location location) {
        spawns.put(name.toLowerCase(), location);
        pendingSpawns.remove(name.toLowerCase()); // Remove from pending if it was there
        String path = name.toLowerCase();
        spawnsConfig.set(path + ".world", location.getWorld().getName());
        spawnsConfig.set(path + ".x", location.getX());
        spawnsConfig.set(path + ".y", location.getY());
        spawnsConfig.set(path + ".z", location.getZ());
        spawnsConfig.set(path + ".yaw", location.getYaw());
        spawnsConfig.set(path + ".pitch", location.getPitch());
        saveSpawnsFile();
    }

    private void saveSpawnsFile() {
        // Also preserve pending spawns in the file so they are never lost
        for (Map.Entry<String, Map<String, Object>> entry : pendingSpawns.entrySet()) {
            String path = entry.getKey();
            Map<String, Object> raw = entry.getValue();
            spawnsConfig.set(path + ".world", raw.get("world"));
            spawnsConfig.set(path + ".x", raw.get("x"));
            spawnsConfig.set(path + ".y", raw.get("y"));
            spawnsConfig.set(path + ".z", raw.get("z"));
            spawnsConfig.set(path + ".yaw", raw.get("yaw"));
            spawnsConfig.set(path + ".pitch", raw.get("pitch"));
        }
        try {
            spawnsConfig.save(spawnsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save spawns.yml: " + e.getMessage());
        }
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
        // Salva prima di spegnere per sicurezza
        if (spawnsConfig != null) {
            saveSpawnsFile();
        }
        spawns.clear();
    }
}
