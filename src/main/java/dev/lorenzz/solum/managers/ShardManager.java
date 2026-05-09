package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.modules.nametag.NametagManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ShardManager {

    private static ShardManager INSTANCE;

    private final Solum plugin;
    private final Map<UUID, Integer> shards = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public ShardManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static ShardManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        DatabaseManager db = DatabaseManager.getInstance();
        if (db != null && db.isUsingSQLite()) {
            shards.putAll(db.getAllShards());
        } else {
            dataFile = new File(plugin.getDataFolder(), "data/shards.yml");
            try {
                Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
                if (!dataFile.exists()) dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            loadFromYaml();
        }
    }

    private void loadFromYaml() {
        if (dataConfig == null || !dataConfig.contains("shards")) return;
        for (String uuid : dataConfig.getConfigurationSection("shards").getKeys(false)) {
            try {
                shards.put(UUID.fromString(uuid), dataConfig.getInt("shards." + uuid));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        DatabaseManager db = DatabaseManager.getInstance();
        if (db != null && db.isUsingSQLite()) {
            for (Map.Entry<UUID, Integer> entry : shards.entrySet()) {
                db.setShards(entry.getKey(), entry.getValue());
            }
        } else if (dataConfig != null) {
            for (Map.Entry<UUID, Integer> entry : shards.entrySet()) {
                dataConfig.set("shards." + entry.getKey().toString(), entry.getValue());
            }
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("[Solum] Failed to save shards: " + e.getMessage());
            }
        }
    }

    public int getShards(UUID uuid) {
        return shards.getOrDefault(uuid, 0);
    }

    public void setShards(UUID uuid, int amount) {
        shards.put(uuid, amount);
        saveAll();

        Player p = plugin.getServer().getPlayer(uuid);
        if (p != null && p.isOnline()) {
            NametagManager.requestRefresh(p);
        }
    }

    public void addShards(UUID uuid, int amount) {
        setShards(uuid, getShards(uuid) + amount);
    }

    public boolean removeShards(UUID uuid, int amount) {
        int current = getShards(uuid);
        if (current < amount) return false;
        setShards(uuid, current - amount);
        return true;
    }

    public List<Map.Entry<UUID, Integer>> getTopShards(int limit) {
        return shards.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public void shutdown() {
        saveAll();
    }
}
