package dev.lorenzz.lxsmp.managers;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StatsManager {

    private static StatsManager INSTANCE;

    private final LXSMP plugin;
    private final Map<UUID, Map<String, Integer>> stats = new HashMap<>();
    private final Map<UUID, Long> playtimeStart = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public StatsManager(LXSMP plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static StatsManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        DatabaseManager db = DatabaseManager.getInstance();
        if (db != null && db.isUsingSQLite()) {
            // SQLite mode - no YAML file needed, data loaded on demand
            plugin.getLogger().info("[Solum] StatsManager using SQLite storage.");
        } else {
            dataFile = new File(plugin.getDataFolder(), "data/stats.yml");
            try {
                Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
                if (!dataFile.exists()) dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            loadAll();
        }
    }

    private void loadAll() {
        if (!dataConfig.contains("stats")) return;
        for (String uuid : dataConfig.getConfigurationSection("stats").getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuid);
                Map<String, Integer> playerStats = new HashMap<>();
                for (String key : dataConfig.getConfigurationSection("stats." + uuid).getKeys(false)) {
                    playerStats.put(key, dataConfig.getInt("stats." + uuid + "." + key));
                }
                stats.put(id, playerStats);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        DatabaseManager db = DatabaseManager.getInstance();
        if (db != null && db.isUsingSQLite()) {
            for (Map.Entry<UUID, Map<String, Integer>> entry : stats.entrySet()) {
                for (Map.Entry<String, Integer> stat : entry.getValue().entrySet()) {
                    db.setStat(entry.getKey(), stat.getKey(), stat.getValue());
                }
            }
        } else if (dataConfig != null) {
            for (Map.Entry<UUID, Map<String, Integer>> entry : stats.entrySet()) {
                String uuid = entry.getKey().toString();
                for (Map.Entry<String, Integer> stat : entry.getValue().entrySet()) {
                    dataConfig.set("stats." + uuid + "." + stat.getKey(), stat.getValue());
                }
            }
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("[Solum] Failed to save stats: " + e.getMessage());
            }
        }
    }

    public int getStat(UUID uuid, String key) {
        return stats.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(key, 0);
    }

    public void setStat(UUID uuid, String key, int value) {
        stats.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, value);
    }

    public void incrementStat(UUID uuid, String key) {
        incrementStat(uuid, key, 1);
    }

    public void incrementStat(UUID uuid, String key, int amount) {
        setStat(uuid, key, getStat(uuid, key) + amount);
    }

    public int getKills(UUID uuid) { return getStat(uuid, "kills"); }
    public int getDeaths(UUID uuid) { return getStat(uuid, "deaths"); }

    public void addKill(UUID uuid) { incrementStat(uuid, "kills"); }
    public void addDeath(UUID uuid) { incrementStat(uuid, "deaths"); }

    public List<Map.Entry<UUID, Integer>> getTopStat(String key, int limit) {
        Map<UUID, Integer> sorted = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Integer>> entry : stats.entrySet()) {
            int val = entry.getValue().getOrDefault(key, 0);
            if (val > 0) sorted.put(entry.getKey(), val);
        }
        return sorted.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) { return getTopStat("kills", limit); }
    public List<Map.Entry<UUID, Integer>> getTopDeaths(int limit) { return getTopStat("deaths", limit); }
    public List<Map.Entry<UUID, Integer>> getTopPlaytime(int limit) { return getTopStat("playtime", limit); }

    public void startPlaytime(UUID uuid) {
        playtimeStart.put(uuid, System.currentTimeMillis());
    }

    public void stopPlaytime(UUID uuid) {
        if (playtimeStart.containsKey(uuid)) {
            long elapsed = (System.currentTimeMillis() - playtimeStart.remove(uuid)) / 1000;
            incrementStat(uuid, "playtime", (int) elapsed);
            saveAll();
        }
    }

    public int getPlaytimeSeconds(UUID uuid) {
        // Prefer PlaytimeRewardsManager (tracks active playtime, excludes AFK)
        PlaytimeRewardsManager prm = PlaytimeRewardsManager.getInstance();
        if (prm != null) {
            return (int) prm.getActivePlaytimeSeconds(uuid);
        }
        int stored = getStat(uuid, "playtime");
        if (playtimeStart.containsKey(uuid)) {
            stored += (int) ((System.currentTimeMillis() - playtimeStart.get(uuid)) / 1000);
        }
        return stored;
    }

    public String getFormattedPlaytime(UUID uuid) {
        // Prefer PlaytimeRewardsManager (tracks active playtime, excludes AFK)
        PlaytimeRewardsManager prm = PlaytimeRewardsManager.getInstance();
        if (prm != null) {
            return prm.getFormattedActivePlaytime(uuid);
        }
        int total = getPlaytimeSeconds(uuid);
        int hours = total / 3600;
        int minutes = (total % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    public void wipeAll() {
        stats.clear();
        playtimeStart.clear();
        DatabaseManager db = DatabaseManager.getInstance();
        if (db != null && db.isUsingSQLite()) {
            db.wipeAllStats();
        } else if (dataConfig != null) {
            dataConfig = new YamlConfiguration();
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, Integer> getPlayerStats(UUID uuid) {
        return Collections.unmodifiableMap(stats.getOrDefault(uuid, Collections.emptyMap()));
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(playtimeStart.keySet())) {
            stopPlaytime(uuid);
        }
        saveAll();
    }
}
