package dev.lorenzz.lxsmp.modules.changelog;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChangelogManager {

    private static ChangelogManager INSTANCE;

    private final LXSMP plugin;
    private File dataFile;
    private YamlConfiguration dataConfig;

    // Ordered list of changelogs (newest first when displayed)
    private final List<ChangelogEntry> changelogs = new ArrayList<>();

    public ChangelogManager(LXSMP plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static ChangelogManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/changelogs.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    private void load() {
        changelogs.clear();
        if (dataConfig == null || !dataConfig.contains("changelogs")) return;
        var section = dataConfig.getConfigurationSection("changelogs");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path = "changelogs." + key;
            String name = dataConfig.getString(path + ".name", "Unknown");
            String version = dataConfig.getString(path + ".version", "1.0");
            List<String> changes = dataConfig.getStringList(path + ".changes");
            String date = dataConfig.getString(path + ".date", "Unknown");
            changelogs.add(new ChangelogEntry(key, name, version, changes, date));
        }
        // Sort by id descending (newest first)
        changelogs.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(b.id()), Integer.parseInt(a.id()));
            } catch (NumberFormatException e) {
                return b.id().compareTo(a.id());
            }
        });
    }

    public void save() {
        if (dataConfig == null) return;
        dataConfig.set("changelogs", null); // Clear
        for (ChangelogEntry entry : changelogs) {
            String path = "changelogs." + entry.id();
            dataConfig.set(path + ".name", entry.name());
            dataConfig.set(path + ".version", entry.version());
            dataConfig.set(path + ".changes", entry.changes());
            dataConfig.set(path + ".date", entry.date());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Solum] Failed to save changelogs: " + e.getMessage());
        }
    }

    public void addChangelog(String name, String version, List<String> changes) {
        // Auto-generate next ID
        int nextId = 1;
        for (ChangelogEntry entry : changelogs) {
            try {
                int id = Integer.parseInt(entry.id());
                if (id >= nextId) nextId = id + 1;
            } catch (NumberFormatException ignored) {}
        }
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        ChangelogEntry entry = new ChangelogEntry(String.valueOf(nextId), name, version, changes, date);
        changelogs.add(0, entry); // Add to front (newest first)
        save();
    }

    public boolean removeChangelog(String id) {
        boolean removed = changelogs.removeIf(e -> e.id().equals(id));
        if (removed) save();
        return removed;
    }

    public List<ChangelogEntry> getChangelogs() {
        return Collections.unmodifiableList(changelogs);
    }

    public void shutdown() {
        save();
    }

    /**
     * A single changelog entry.
     */
    public record ChangelogEntry(String id, String name, String version, List<String> changes, String date) {}
}
