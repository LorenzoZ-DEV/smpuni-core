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
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

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

        plugin.getLogger().info("[Solum] NametagManager started (offset=" + yOffset
                + ", update-interval=" + updateIntervalTicks + " ticks).");
    }

    public void stop() {
        if (updateTask != null) { updateTask.cancel(); updateTask = null; }

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

        // Spawn at the player's location — the visual offset is achieved via the
        // display's translation, NOT by teleporting it every tick. Once mounted as
        // a passenger of the player, it follows perfectly with zero jitter.
        Location loc = player.getLocation();
        TextDisplay td = player.getWorld().spawn(loc, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setSeeThrough(false);
            t.setShadowed(false);
            t.setPersistent(false);
            t.setViewRange(1.0f);
            Transformation tr = t.getTransformation();
            t.setTransformation(new Transformation(
                    new Vector3f(0f, (float) yOffset, 0f),
                    tr.getLeftRotation(),
                    tr.getScale(),
                    tr.getRightRotation()
            ));
            t.setInterpolationDuration(0);
            t.setTeleportDuration(0);
            t.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING,
                    player.getUniqueId().toString());
            t.text(buildComponent(player));
        });

        // Mount on the player so the entity is perfectly synced client-side.
        try { player.addPassenger(td); } catch (Throwable ignored) {}

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
            // Re-mount if dismounted somehow (vehicle change, dimension teleport, etc.).
            if (td.getVehicle() == null) {
                try { p.addPassenger(td); } catch (Throwable ignored) {}
            }
            td.text(buildComponent(p));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!displays.containsKey(p.getUniqueId())) attach(p);
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
