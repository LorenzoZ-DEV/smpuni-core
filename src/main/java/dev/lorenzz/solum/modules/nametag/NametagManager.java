package dev.lorenzz.solum.modules.nametag;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a {@link TextDisplay} entity per online player, rendered just below the
 * vanilla nametag, showing shards / health / team info on a separate line.
 *
 * The display is teleported every tick to follow the owning player. It is invisible
 * to the owner (so they don't see their own stats floating in front of them).
 */
public class NametagManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('\u00A7')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static final String DEFAULT_FORMAT =
            "&e\uD83E\uDE99 %solum_shards% &7|| &c\u2764 %player_health_rounded%%solum_team_tag_sep%";

    private final Solum plugin;
    private final NamespacedKey markerKey;
    private final Map<UUID, UUID> displays = new ConcurrentHashMap<>(); // player -> display

    private BukkitTask updateTask;
    private BukkitTask followTask;

    private double yOffset = 1.7;
    private long updateIntervalTicks = 20L;
    private String format = DEFAULT_FORMAT;
    private boolean hideFromOwner = true;

    public NametagManager(Solum plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "nametag_display");
    }

    public void start() {
        loadConfig();
        cleanupOrphans();

        for (Player p : Bukkit.getOnlinePlayers()) attach(p);

        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll,
                updateIntervalTicks, updateIntervalTicks);
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::followAll, 1L, 1L);

        plugin.getLogger().info("[Solum] NametagManager started (offset=" + yOffset
                + ", update-interval=" + updateIntervalTicks + " ticks).");
    }

    public void stop() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }
        if (followTask != null) { followTask.cancel(); followTask = null; }

        for (UUID id : displays.values()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        displays.clear();
        cleanupOrphans();
    }

    public void reload() {
        stop();
        start();
    }

    private void loadConfig() {
        org.bukkit.configuration.ConfigurationSection cfg = plugin.getConfigFile();
        this.yOffset = cfg.getDouble("nametag.y-offset", 1.7);
        this.updateIntervalTicks = Math.max(1L, cfg.getLong("nametag.update-interval-ticks", 20L));
        this.format = cfg.getString("nametag.format", DEFAULT_FORMAT);
        this.hideFromOwner = cfg.getBoolean("nametag.hide-from-owner", true);
    }

    public void attach(Player player) {
        if (player == null || !player.isOnline()) return;
        if (displays.containsKey(player.getUniqueId())) return;

        Location loc = player.getLocation().clone().add(0, yOffset, 0);
        TextDisplay td = player.getWorld().spawn(loc, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setSeeThrough(false);
            t.setShadowed(false);
            t.setPersistent(false);
            t.setViewRange(0.6f); // tighter than default
            t.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING,
                    player.getUniqueId().toString());
            t.text(buildComponent(player));
        });

        if (hideFromOwner) {
            try { player.hideEntity(plugin, td); } catch (Throwable ignored) {}
        }

        displays.put(player.getUniqueId(), td.getUniqueId());
    }

    public void detach(Player player) {
        if (player == null) return;
        UUID id = displays.remove(player.getUniqueId());
        if (id == null) return;
        Entity e = Bukkit.getEntity(id);
        if (e != null) e.remove();
    }

    private void updateAll() {
        Iterator<Map.Entry<UUID, UUID>> it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) {
                Entity e = Bukkit.getEntity(entry.getValue());
                if (e != null) e.remove();
                it.remove();
                continue;
            }
            Entity e = Bukkit.getEntity(entry.getValue());
            if (!(e instanceof TextDisplay td) || td.isDead()) {
                it.remove();
                attach(p);
                continue;
            }
            td.text(buildComponent(p));
        }

        // Attach for any online players that don't have a display yet
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!displays.containsKey(p.getUniqueId())) attach(p);
        }
    }

    private void followAll() {
        for (Map.Entry<UUID, UUID> entry : displays.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;
            Entity e = Bukkit.getEntity(entry.getValue());
            if (!(e instanceof TextDisplay td) || td.isDead()) continue;

            Location target = p.getLocation();
            // Use a fresh location to avoid mutating player's reference
            Location dest = new Location(target.getWorld(),
                    target.getX(), target.getY() + yOffset, target.getZ(),
                    target.getYaw(), 0f);

            if (td.getWorld() != dest.getWorld()) {
                td.teleport(dest);
            } else {
                td.teleport(dest);
            }
        }
    }

    private Component buildComponent(Player p) {
        String s = format == null ? "" : format;
        s = s.replace("%player%", p.getName())
             .replace("%player_name%", p.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try { s = PlaceholderAPI.setPlaceholders(p, s); } catch (Throwable ignored) {}
        }
        return LEGACY.deserialize(Messages.color(s));
    }

    /** Removes any leftover TextDisplay entities tagged by us (e.g. after a crash). */
    private void cleanupOrphans() {
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntitiesByClass(TextDisplay.class)) {
                if (e.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)) {
                    e.remove();
                }
            }
        }
    }
}

