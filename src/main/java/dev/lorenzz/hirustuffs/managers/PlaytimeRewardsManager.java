package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
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

/**
 * Manages playtime rewards. Tracks ACTIVE playtime (excludes AFK time)
 * and awards milestone rewards.
 */
public class PlaytimeRewardsManager {

    private static PlaytimeRewardsManager INSTANCE;

    private final Hiru plugin;
    private final Logger logger;

    // Milestone definitions from config
    private final List<Milestone> milestones = new ArrayList<>();
    // uuid -> set of milestone minutes already claimed
    private final Map<UUID, Set<Integer>> claimedMilestones = new HashMap<>();
    // uuid -> active playtime in seconds (excludes AFK time)
    private final Map<UUID, Long> activePlaytime = new ConcurrentHashMap<>();
    // uuid -> timestamp when active tracking started
    private final Map<UUID, Long> trackingStart = new ConcurrentHashMap<>();

    private BukkitTask tickTask;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public record Milestone(
            int minutes,
            String displayName,
            String icon, // Material name
            int moneyReward,
            int shardsReward,
            List<String> commands // Commands to execute (use %player%)
    ) implements Comparable<Milestone> {
        @Override
        public int compareTo(Milestone o) {
            return Integer.compare(this.minutes, o.minutes);
        }
    }

    public PlaytimeRewardsManager(Hiru plugin) {
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
            logger.warning("[Hiru] No playtime reward milestones found in config.");
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
        logger.info("[Hiru] Loaded " + milestones.size() + " playtime reward milestones.");
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

        // First flush active tracking
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
            logger.severe("[Hiru] Failed to save playtime-rewards data: " + e.getMessage());
        }
    }

    /**
     * Starts a repeating task that checks every 30 seconds.
     * - Increments active playtime for non-AFK players
     * - Checks for unclaimed milestones and notifies
     */
    private void startTrackingTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            AFKManager afk = AFKManager.getInstance();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                // If AFK, DON'T accumulate active playtime
                boolean isAfk = afk != null && afk.isAFK(uuid);

                if (isAfk) {
                    // Pause tracking — flush what we have
                    if (trackingStart.containsKey(uuid)) {
                        flushTracking(uuid);
                        trackingStart.remove(uuid);
                    }
                } else {
                    // Start tracking if not already
                    trackingStart.putIfAbsent(uuid, System.currentTimeMillis());
                }

                // Check for available milestones and notify
                long totalMinutes = getActivePlaytimeMinutes(uuid);
                for (Milestone ms : milestones) {
                    if (totalMinutes >= ms.minutes() && !hasClaimed(uuid, ms.minutes())) {
                        // Notify player that a reward is available
                        dev.lorenzz.hirustuffs.utils.Messages.send(player,
                                "&#FFD700⚡ &#E0E0E0You have an unclaimed playtime reward! Use &#55FF55/rewards &#E0E0E0to claim it.");
                        break; // Only notify once per tick
                    }
                }
            }
        }, 20L * 30, 20L * 30); // Every 30 seconds
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
        save();
    }

    /**
     * Returns total active playtime in minutes for the player.
     */
    public long getActivePlaytimeMinutes(UUID uuid) {
        long total = activePlaytime.getOrDefault(uuid, 0L);
        // Add current session time if tracking
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

    /**
     * Claims a milestone reward for a player. Returns true on success.
     */
    public boolean claimMilestone(Player player, int milestoneMinutes) {
        UUID uuid = player.getUniqueId();

        // Find milestone
        Milestone milestone = milestones.stream()
                .filter(m -> m.minutes() == milestoneMinutes)
                .findFirst().orElse(null);
        if (milestone == null) return false;

        // Check playtime
        if (getActivePlaytimeMinutes(uuid) < milestone.minutes()) return false;

        // Check not already claimed
        if (hasClaimed(uuid, milestoneMinutes)) return false;

        // Grant rewards
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

        // Mark as claimed
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
