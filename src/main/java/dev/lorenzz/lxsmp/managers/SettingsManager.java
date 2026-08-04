package dev.lorenzz.lxsmp.managers;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsManager {

    private static SettingsManager INSTANCE;

    private final LXSMP plugin;
    private final Map<UUID, Map<String, Boolean>> playerSettings = new ConcurrentHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public static final String[] SETTING_KEYS = {
            "chat", "private-messages", "tpa-requests",
            "bounty-notifications", "team-notifications", "particles",
            "kill-messages", "sell-notifications"
    };

    public SettingsManager(LXSMP plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static SettingsManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/settings.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (!dataConfig.contains("settings")) return;
        var section = dataConfig.getConfigurationSection("settings");
        if (section == null) return;
        for (String uuid : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(uuid);
                Map<String, Boolean> map = new HashMap<>();
                var playerSection = section.getConfigurationSection(uuid);
                if (playerSection != null) {
                    for (String key : playerSection.getKeys(false)) {
                        map.put(key, playerSection.getBoolean(key));
                    }
                }
                playerSettings.put(id, map);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, Map<String, Boolean>> entry : playerSettings.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, Boolean> setting : entry.getValue().entrySet()) {
                dataConfig.set("settings." + uuid + "." + setting.getKey(), setting.getValue());
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Solum] Failed to save settings: " + e.getMessage());
        }
    }

    public boolean getSetting(UUID uuid, String key) {
        return playerSettings.getOrDefault(uuid, Collections.emptyMap()).getOrDefault(key, true);
    }

    public void setSetting(UUID uuid, String key, boolean value) {
        playerSettings.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, value);
    }

    public void toggleSetting(UUID uuid, String key) {
        setSetting(uuid, key, !getSetting(uuid, key));
        saveAll();
    }

    public Map<String, Boolean> getAllSettings(UUID uuid) {
        Map<String, Boolean> result = new HashMap<>();
        for (String key : SETTING_KEYS) {
            result.put(key, getSetting(uuid, key));
        }
        return result;
    }

    public void shutdown() {
        saveAll();
    }
}
