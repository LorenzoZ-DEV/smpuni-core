package dev.lorenzz.solum.modules.announcements;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Config;
import dev.lorenzz.solum.utils.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Manages automatic broadcast announcements loaded from announcements.yml.
 */
public class AnnouncementManager {

    private static AnnouncementManager INSTANCE;

    private final Solum plugin;
    private final Logger logger;

    private Config announcementsConfig;
    private BukkitTask broadcastTask;

    private int intervalSeconds;
    private String mode; // SEQUENTIAL or RANDOM

    // Category ID -> AnnouncementCategory
    private final Map<String, AnnouncementCategory> categories = new LinkedHashMap<>();
    // Flattened list of all enabled announcements (prefix + message)
    private final List<PreparedAnnouncement> pool = new ArrayList<>();
    private int sequentialIndex = 0;

    public AnnouncementManager(Solum plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        INSTANCE = this;
    }

    public static AnnouncementManager getInstance() {
        return INSTANCE;
    }

    public void load() {
        announcementsConfig = new Config(plugin, "announcements.yml");
        reload();
    }

    public void reload() {
        // Stop existing task
        stopTask();

        categories.clear();
        pool.clear();
        sequentialIndex = 0;

        if (announcementsConfig == null) {
            announcementsConfig = new Config(plugin, "announcements.yml");
        } else {
            announcementsConfig.reload();
        }

        intervalSeconds = announcementsConfig.getInt("interval-seconds", 120);
        mode = announcementsConfig.getString("mode", "SEQUENTIAL").toUpperCase();

        ConfigurationSection catSection = announcementsConfig.getConfigurationSection("categories");
        if (catSection == null) {
            logger.warning("[Solum] No announcement categories found in announcements.yml!");
            return;
        }

        for (String catId : catSection.getKeys(false)) {
            ConfigurationSection sec = catSection.getConfigurationSection(catId);
            if (sec == null) continue;

            String prefix = sec.getString("prefix", "&7[Announce] ");
            boolean enabled = sec.getBoolean("enabled", true);
            List<String> messages = sec.getStringList("messages");

            AnnouncementCategory category = new AnnouncementCategory(catId, prefix, enabled, messages);
            categories.put(catId.toLowerCase(), category);

            if (enabled) {
                for (String msg : messages) {
                    pool.add(new PreparedAnnouncement(catId, prefix, msg));
                }
            }
        }

        logger.info("[Solum] Loaded " + categories.size() + " announcement categories (" + pool.size() + " messages).");

        // Start task
        startTask();
    }

    public void shutdown() {
        stopTask();
        INSTANCE = null;
    }

    // ── Task Management ──

    private void startTask() {
        if (pool.isEmpty() || intervalSeconds <= 0) return;

        long intervalTicks = intervalSeconds * 20L;

        broadcastTask = new BukkitRunnable() {
            @Override
            public void run() {
                broadcastNext();
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);
    }

    private void stopTask() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
            broadcastTask = null;
        }
    }

    private void broadcastNext() {
        if (pool.isEmpty()) return;

        PreparedAnnouncement announcement;

        if ("RANDOM".equals(mode)) {
            announcement = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        } else {
            announcement = pool.get(sequentialIndex);
            sequentialIndex = (sequentialIndex + 1) % pool.size();
        }

        String fullMessage = announcement.prefix() + announcement.message();
        Component component = Messages.colorComponent(fullMessage);

        // Separator line
        Component separator = Messages.colorComponent("&8&m                                                    ");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(Component.empty());
            player.sendMessage(separator);
            player.sendMessage(component);
            player.sendMessage(separator);
        }
    }

    // ── Toggle Categories ──

    /**
     * Toggle a category on/off at runtime. Also persists to config file.
     */
    public boolean toggleCategory(String categoryId) {
        AnnouncementCategory cat = categories.get(categoryId.toLowerCase());
        if (cat == null) return false;

        boolean newState = !cat.isEnabled();
        cat.setEnabled(newState);

        // Save to config
        announcementsConfig.set("categories." + categoryId.toLowerCase() + ".enabled", newState);
        announcementsConfig.saveFile();

        // Rebuild pool
        rebuildPool();
        return newState;
    }

    /**
     * Check if a category exists.
     */
    public boolean categoryExists(String categoryId) {
        return categories.containsKey(categoryId.toLowerCase());
    }

    /**
     * Get a category by id.
     */
    public AnnouncementCategory getCategory(String categoryId) {
        return categories.get(categoryId.toLowerCase());
    }

    /**
     * Get all categories.
     */
    public Collection<AnnouncementCategory> getCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    private void rebuildPool() {
        pool.clear();
        sequentialIndex = 0;
        for (AnnouncementCategory cat : categories.values()) {
            if (cat.isEnabled()) {
                for (String msg : cat.getMessages()) {
                    pool.add(new PreparedAnnouncement(cat.getId(), cat.getPrefix(), msg));
                }
            }
        }
    }

    // ── Data classes ──

    public record PreparedAnnouncement(String categoryId, String prefix, String message) {}

    public static class AnnouncementCategory {
        private final String id;
        private final String prefix;
        private boolean enabled;
        private final List<String> messages;

        public AnnouncementCategory(String id, String prefix, boolean enabled, List<String> messages) {
            this.id = id;
            this.prefix = prefix;
            this.enabled = enabled;
            this.messages = messages;
        }

        public String getId() { return id; }
        public String getPrefix() { return prefix; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getMessages() { return messages; }
    }
}
