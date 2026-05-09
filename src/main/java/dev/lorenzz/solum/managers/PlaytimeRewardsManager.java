package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlaytimeRewardsManager {

    private static PlaytimeRewardsManager INSTANCE;

    private final Solum plugin;
    private final Logger logger;

    private final List<Milestone> milestones = new ArrayList<>();
    private final Map<UUID, Set<Integer>> claimedMilestones = new HashMap<>();
    private final Map<UUID, Long> activePlaytime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> trackingStart = new ConcurrentHashMap<>();

    private BukkitTask tickTask;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public record Milestone(
            int minutes,
            String displayName,
            String icon,
            int moneyReward,
            int shardsReward,
            List<String> commands
    ) implements Comparable<Milestone> {
        @Override
        public int compareTo(Milestone o) {
            return Integer.compare(this.minutes, o.minutes);
        }
    }

    public PlaytimeRewardsManager(Solum plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        INSTANCE = this;
    }

    public static PlaytimeRewardsManager getInstance() { return INSTANCE; }

    public void init() {
        loadMilestones();
        loadPlayerData();
        startTrackingTask();
    }

    private void loadMilestones() {
        milestones.clear();
        ConfigurationSection section = plugin.getConfigFile().getConfigurationSection("playtime-rewards.milestones");
        if (section == null) {
            logger.warning("[Solum] No playtime reward milestones found in config.");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection ms = section.getConfigurationSection(key);
            if (ms == null) continue;
            milestones.add(new Milestone(
                    ms.getInt("minutes", 60),
                    ms.getString("display-name", "&f" + key),
                    ms.getString("icon", "CLOCK"),
                    ms.getInt("reward-money", 0),
                    ms.getInt("reward-shards", 0),
                    ms.getStringList("reward-commands")
            ));
        }

        milestones.sort(Comparator.naturalOrder());
        logger.info("[Solum] Loaded " + milestones.size() + " playtime reward milestones.");
    }

    private void loadPlayerData() {
        claimedMilestones.clear();
        activePlaytime.clear();

        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
        } catch (IOException e) { e.printStackTrace(); }

        dataFile = new File(plugin.getDataFolder(), "data/playtime-rewards.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { continue; }

            ConfigurationSection ps = players.getConfigurationSection(uuidStr);
            if (ps == null) continue;

            long stored = ps.getLong("active-playtime", 0);
            activePlaytime.put(uuid, stored);

            List<Integer> claimed = ps.getIntegerList("claimed");
            claimedMilestones.put(uuid, new HashSet<>(claimed));
        }
    }

    public void save() {
        if (dataConfig == null) return;

        for (UUID uuid : trackingStart.keySet()) {
            flushTracking(uuid);
        }

        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(activePlaytime.keySet());
        allUuids.addAll(claimedMilestones.keySet());

        for (UUID uuid : allUuids) {
            String base = "players." + uuid.toString();
            dataConfig.set(base + ".active-playtime", activePlaytime.getOrDefault(uuid, 0L));
            Set<Integer> claimed = claimedMilestones.get(uuid);
            dataConfig.set(base + ".claimed", claimed != null ? new ArrayList<>(claimed) : List.of());
        }

        try { dataConfig.save(dataFile); } catch (IOException e) {
            logger.severe("[Solum] Failed to save playtime-rewards data: " + e.getMessage());
        }
    }

    private void startTrackingTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            AFKManager afk = AFKManager.getInstance();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                boolean isAfk = afk != null && afk.isAFK(uuid);

                if (isAfk) {
                    if (trackingStart.containsKey(uuid)) {
                        flushTracking(uuid);
                        trackingStart.remove(uuid);
                    }
                } else {
                    trackingStart.putIfAbsent(uuid, System.currentTimeMillis());
                }

                long totalMinutes = getActivePlaytimeMinutes(uuid);
                for (Milestone ms : milestones) {
                    if (totalMinutes >= ms.minutes() && !hasClaimed(uuid, ms.minutes())) {
                        dev.lorenzz.solum.utils.Messages.send(player,
                                "&#FFD700⚡ &#E0E0E0You have an unclaimed playtime reward! Use &#55FF55/rewards &#E0E0E0to claim it.");
                        break;
                    }
                }
            }
        }, 20L * 30, 20L * 30);
    }

    private void flushTracking(UUID uuid) {
        Long start = trackingStart.get(uuid);
        if (start == null) return;
        long elapsed = (System.currentTimeMillis() - start) / 1000;
        activePlaytime.merge(uuid, elapsed, Long::sum);
        trackingStart.put(uuid, System.currentTimeMillis());
    }

    public void playerJoin(UUID uuid) {
        activePlaytime.putIfAbsent(uuid, 0L);
        trackingStart.put(uuid, System.currentTimeMillis());
    }

    public void playerQuit(UUID uuid) {
        flushTracking(uuid);
        trackingStart.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public long getActivePlaytimeMinutes(UUID uuid) {
        long total = activePlaytime.getOrDefault(uuid, 0L);
        Long start = trackingStart.get(uuid);
        if (start != null) {
            total += (System.currentTimeMillis() - start) / 1000;
        }
        return total / 60;
    }

    public long getActivePlaytimeSeconds(UUID uuid) {
        long total = activePlaytime.getOrDefault(uuid, 0L);
        Long start = trackingStart.get(uuid);
        if (start != null) {
            total += (System.currentTimeMillis() - start) / 1000;
        }
        return total;
    }

    public String getFormattedActivePlaytime(UUID uuid) {
        long seconds = getActivePlaytimeSeconds(uuid);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    public boolean hasClaimed(UUID uuid, int milestoneMinutes) {
        Set<Integer> claimed = claimedMilestones.get(uuid);
        return claimed != null && claimed.contains(milestoneMinutes);
    }

    public boolean claimMilestone(Player player, int milestoneMinutes) {
        UUID uuid = player.getUniqueId();

        Milestone milestone = milestones.stream()
                .filter(m -> m.minutes() == milestoneMinutes)
                .findFirst().orElse(null);
        if (milestone == null) return false;

        if (getActivePlaytimeMinutes(uuid) < milestone.minutes()) return false;

        if (hasClaimed(uuid, milestoneMinutes)) return false;

        if (milestone.moneyReward() > 0) {
            EconomyManager eco = EconomyManager.getInstance();
            if (eco != null) eco.addBalance(uuid, milestone.moneyReward());
        }
        if (milestone.shardsReward() > 0) {
            ShardManager sm = ShardManager.getInstance();
            if (sm != null) sm.addShards(uuid, milestone.shardsReward());
        }
        for (String cmd : milestone.commands()) {
            String resolved = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }

        claimedMilestones.computeIfAbsent(uuid, k -> new HashSet<>()).add(milestoneMinutes);
        save();
        return true;
    }

    public List<Milestone> getMilestones() {
        return Collections.unmodifiableList(milestones);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        save();
    }
}
