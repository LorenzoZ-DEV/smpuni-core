package dev.lorenzz.solum.modules.kits;

import dev.lorenzz.solum.Solum;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Tracks per-player kit data: last claim timestamps, one-time claims.
 */
public class KitPlayerDataManager {

    private static KitPlayerDataManager INSTANCE;

    private final Solum plugin;
    private final Logger logger;
    private File dataFile;
    private YamlConfiguration dataConfig;

    // UUID -> kit id -> last claim millis
    private final Map<UUID, Map<String, Long>> lastClaim = new HashMap<>();
    // UUID -> set of one-time kit ids already claimed
    private final Map<UUID, Set<String>> oneTimeClaimed = new HashMap<>();

    public KitPlayerDataManager(Solum plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        INSTANCE = this;
    }

    public static KitPlayerDataManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        lastClaim.clear();
        oneTimeClaimed.clear();

        // Create data directory and file manually (not an embedded resource)
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        dataFile = new File(dataDir, "kit-playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                logger.severe("[Solum] Failed to create kit-playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) return;

        for (String uuidStr : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            ConfigurationSection pSec = players.getConfigurationSection(uuidStr);
            if (pSec == null) continue;

            // Cooldowns
            ConfigurationSection cooldowns = pSec.getConfigurationSection("cooldowns");
            if (cooldowns != null) {
                Map<String, Long> map = new HashMap<>();
                for (String kitId : cooldowns.getKeys(false)) {
                    map.put(kitId.toLowerCase(), cooldowns.getLong(kitId));
                }
                lastClaim.put(uuid, map);
            }

            // One-time
            List<String> claimed = pSec.getStringList("one-time-claimed");
            if (!claimed.isEmpty()) {
                Set<String> set = new HashSet<>();
                for (String s : claimed) set.add(s.toLowerCase());
                oneTimeClaimed.put(uuid, set);
            }
        }
    }

    public void save() {
        if (dataConfig == null) return;

        // Collect all UUIDs
        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(lastClaim.keySet());
        allUuids.addAll(oneTimeClaimed.keySet());

        for (UUID uuid : allUuids) {
            String base = "players." + uuid.toString();

            Map<String, Long> cooldowns = lastClaim.get(uuid);
            if (cooldowns != null && !cooldowns.isEmpty()) {
                for (Map.Entry<String, Long> e : cooldowns.entrySet()) {
                    dataConfig.set(base + ".cooldowns." + e.getKey(), e.getValue());
                }
            }

            Set<String> claimed = oneTimeClaimed.get(uuid);
            if (claimed != null && !claimed.isEmpty()) {
                dataConfig.set(base + ".one-time-claimed", new ArrayList<>(claimed));
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            logger.severe("[Solum] Failed to save kit-playerdata.yml: " + e.getMessage());
        }
    }

    /**
     * Returns remaining cooldown in seconds, or 0 if ready.
     */
    public long getRemainingCooldown(UUID uuid, Kit kit) {
        if (kit.getCooldown() <= 0) return 0;

        Map<String, Long> map = lastClaim.get(uuid);
        if (map == null) return 0;

        Long last = map.get(kit.getId().toLowerCase());
        if (last == null) return 0;

        long elapsed = (System.currentTimeMillis() - last) / 1000L;
        long remaining = kit.getCooldown() - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Returns true if this one-time kit was already claimed.
     */
    public boolean hasClaimedOneTime(UUID uuid, String kitId) {
        Set<String> set = oneTimeClaimed.get(uuid);
        return set != null && set.contains(kitId.toLowerCase());
    }

    /**
     * Records a kit claim for the player.
     */
    public void recordClaim(UUID uuid, Kit kit) {
        // Cooldown timestamp
        lastClaim.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(kit.getId().toLowerCase(), System.currentTimeMillis());

        // One-time
        if (kit.isOneTime()) {
            oneTimeClaimed.computeIfAbsent(uuid, k -> new HashSet<>())
                    .add(kit.getId().toLowerCase());
        }

        // Auto-save
        save();
    }

    /**
     * Formats seconds into a human-readable string (e.g. "2h 30m 10s").
     */
    public static String formatCooldown(long seconds) {
        if (seconds <= 0) return "Ready";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        sb.append(s).append("s");
        return sb.toString().trim();
    }
}
