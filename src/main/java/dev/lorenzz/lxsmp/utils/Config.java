package dev.lorenzz.lxsmp.utils;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config extends YamlConfiguration {

    private final Plugin plugin;
    private final String fileName;
    private final Path filePath;

    public Config(Plugin plugin, String fileName) {
        this(plugin, fileName, true);
    }

    public Config(Plugin plugin, String fileName, boolean load) {
        this.plugin = plugin;
        this.fileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        this.filePath = Path.of(plugin.getDataFolder().getPath(), this.fileName);
        ensureDataFolder();

        try {
            if (Files.notExists(this.filePath)) {
                plugin.saveResource(this.fileName, false);
            }

            if (load) {
                super.load(this.filePath.toFile());
                InputStream resource = plugin.getResource(this.fileName);
                if (resource != null) {
                    try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                        this.setDefaults(defaults);
                        this.options().copyDefaults(true);
                    }
                }
            }
        } catch (InvalidConfigurationException | IOException ex) {
            plugin.getLogger().severe("Failed to load config: " + this.fileName + " - " + ex.getMessage());
        }
    }

    private void ensureDataFolder() {
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        try {
            super.load(this.filePath.toFile());
            InputStream resource = plugin.getResource(this.fileName);
            if (resource != null) {
                try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                    this.setDefaults(defaults);
                    this.options().copyDefaults(true);
                }
            }
            plugin.getLogger().info("Reloaded config: " + this.fileName);
        } catch (InvalidConfigurationException | IOException ex) {
            plugin.getLogger().severe("Failed to reload config: " + this.fileName + " - " + ex.getMessage());
        }
    }

    public void saveFile() {
        try {
            super.save(this.filePath.toFile());
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save config: " + this.fileName + " - " + ex.getMessage());
        }
    }

    public Path getFilePath() {
        return filePath;
    }
}
