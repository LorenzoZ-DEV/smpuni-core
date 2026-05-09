package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BountyManager {

    private static BountyManager INSTANCE;

    private final Solum plugin;
    private final Map<UUID, Bounty> bounties = new ConcurrentHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public record Bounty(UUID target, UUID setter, int amount, long timestamp) {}

    public BountyManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static BountyManager getInstance() { return INSTANCE; }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/bounties.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) { e.printStackTrace(); }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (!dataConfig.contains("bounties")) return;
        ConfigurationSection sec = dataConfig.getConfigurationSection("bounties");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                UUID target = UUID.fromString(key);
                UUID setter = UUID.fromString(sec.getString(key + ".setter", ""));
                int amount = sec.getInt(key + ".amount");
                long timestamp = sec.getLong(key + ".timestamp");
                bounties.put(target, new Bounty(target, setter, amount, timestamp));
            } catch (Exception ignored) {}
        }
    }

    public void saveAll() {
        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Bounty> entry : bounties.entrySet()) {
            Bounty b = entry.getValue();
            String path = "bounties." + b.target().toString();
            dataConfig.set(path + ".setter", b.setter().toString());
            dataConfig.set(path + ".amount", b.amount());
            dataConfig.set(path + ".timestamp", b.timestamp());
        }
        try { dataConfig.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("[Solum] Failed to save bounties: " + e.getMessage());
        }
    }

    public boolean placeBounty(UUID setter, UUID target, int amount) {
        int min = plugin.getConfigFile().getInt("bounty.min-amount", 50);
        if (amount < min) return false;

        Bounty existing = bounties.get(target);
        int total = existing != null ? existing.amount() + amount : amount;
        bounties.put(target, new Bounty(target, setter, total, System.currentTimeMillis()));
        saveAll();
        return true;
    }

    public Bounty claimBounty(UUID target) {
        Bounty bounty = bounties.remove(target);
        if (bounty != null) saveAll();
        return bounty;
    }

    public Bounty getBounty(UUID target) {
        return bounties.get(target);
    }

    public boolean hasBounty(UUID target) {
        return bounties.containsKey(target);
    }

    public List<Bounty> getAllBounties() {
        return bounties.values().stream()
                .sorted(Comparator.comparingInt(Bounty::amount).reversed())
                .collect(Collectors.toList());
    }

    public List<Bounty> getBountiesSortedByName() {
        return bounties.values().stream()
                .sorted(Comparator.comparing(b -> org.bukkit.Bukkit.getOfflinePlayer(b.target()).getName() != null ?
                        org.bukkit.Bukkit.getOfflinePlayer(b.target()).getName() : ""))
                .collect(Collectors.toList());
    }

    public int getBountyCount() { return bounties.size(); }

    public void shutdown() { saveAll(); }
}
