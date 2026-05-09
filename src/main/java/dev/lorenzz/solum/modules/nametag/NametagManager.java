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
    private BukkitTask followTask;

    /**
     * Vertical distance ABOVE the player's eye height where the TextDisplay anchor
     * is placed. With Billboard.CENTER + scale 1.0 the text extends ~0.25 blocks
     * upward from this point, so the rendered top sits ~(eye + extra + 0.25) above
     * the feet — i.e. just below the vanilla nameplate base (~eye + 0.50).
     *
     * Default 0.18 → top of stats line ≈ eye + 0.43 → flush below nameplate (eye + 0.50).
     * Tweak ±0.02 to taste. Using eye height (not a fixed Y) means sneak / swim /
     * crawl are handled automatically: the line follows the player's head pose.
     */
    private double extraAboveEyes = -0.07;
    private long updateIntervalTicks = 20L;
    private int teleportDuration = 3;
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
        // TAB-style follow: teleport every tick; client-side interpolation
        // (setTeleportDuration) smooths the motion → zero jitter, perfect sync.
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::followAll, 1L, 1L);

        plugin.getLogger().info("[Solum] NametagManager started (extra-above-eyes="
                + extraAboveEyes + ", teleport-duration=" + teleportDuration + ").");
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
        // New key (preferred). Fallback to legacy 'y-offset' converted from absolute → relative.
        if (cfg.isSet("nametag.extra-above-eyes")) {
            this.extraAboveEyes = cfg.getDouble("nametag.extra-above-eyes", -0.07);
        } else if (cfg.isSet("nametag.y-offset")) {
            // Legacy: y-offset was absolute from feet. Convert: extra = y-offset - 1.62 (eye standing).
            this.extraAboveEyes = cfg.getDouble("nametag.y-offset", 1.55) - 1.62;
        } else {
            this.extraAboveEyes = -0.07;
        }
        this.updateIntervalTicks = Math.max(1L, cfg.getLong("nametag.update-interval-ticks", 20L));
        this.teleportDuration = Math.max(0, cfg.getInt("nametag.teleport-duration", 3));
        this.format = cfg.getString("nametag.format", DEFAULT_FORMAT);
        this.hideFromOwner = cfg.getBoolean("nametag.hide-from-owner", true);
    }

    /** Live setter — used by the admin command for runtime tuning without reload. */
    public void setExtraAboveEyes(double value) {
        this.extraAboveEyes = value;
        plugin.getConfig().set("nametag.extra-above-eyes", value);
        plugin.saveConfig();
    }

    public double getExtraAboveEyes() { return extraAboveEyes; }

    /** Live toggle of the "hide from owner" behavior. Re-applies visibility to all online players. */
    public void setHideFromOwner(boolean hide) {
        this.hideFromOwner = hide;
        plugin.getConfig().set("nametag.hide-from-owner", hide);
        plugin.saveConfig();
        for (Map.Entry<UUID, UUID> entry : displays.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            Entity e = Bukkit.getEntity(entry.getValue());
            if (!(e instanceof TextDisplay td)) continue;
            try {
                if (hide) p.hideEntity(plugin, td);
                else p.showEntity(plugin, td);
            } catch (Throwable ignored) {}
        }
    }

    public boolean isHideFromOwner() { return hideFromOwner; }

    /** Anchor Y for the TextDisplay relative to player feet. */
    private double computeAnchorY(Player p) {
        return p.getEyeHeight() + extraAboveEyes;
    }

    public void attach(Player player) {
        if (player == null || !player.isOnline()) return;
        if (displays.containsKey(player.getUniqueId())) return;

        Location loc = player.getLocation().clone();
        loc.setY(loc.getY() + computeAnchorY(player));
        loc.setYaw(0f);
        loc.setPitch(0f);

        TextDisplay td = player.getWorld().spawn(loc, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setSeeThrough(false);
            t.setShadowed(false);
            t.setPersistent(false);
            t.setViewRange(1.0f);
            // No translation — position handled directly by entity location.
            Transformation tr = t.getTransformation();
            t.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    tr.getLeftRotation(),
                    tr.getScale(),
                    tr.getRightRotation()
            ));
            t.setInterpolationDuration(0);
            // Smooth teleport interpolation (Paper 1.20.2+) — TAB uses the same trick.
            try { t.setTeleportDuration(teleportDuration); } catch (Throwable ignored) {}
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

    /** Per-tick: glue each TextDisplay to its player's head. Client interpolates. */
    private void followAll() {
        for (Map.Entry<UUID, UUID> entry : displays.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;
            Entity e = Bukkit.getEntity(entry.getValue());
            if (!(e instanceof TextDisplay td) || td.isDead()) continue;

            Location target = p.getLocation().clone();
            // Eye-relative anchor → automatically follows sneak / swim / crawl
            // (no manual sneak compensation needed).
            target.setY(target.getY() + computeAnchorY(p));
            target.setYaw(0f);
            target.setPitch(0f);

            if (!td.getWorld().equals(target.getWorld())) {
                td.remove();
                displays.remove(entry.getKey());
                attach(p);
                continue;
            }
            td.teleport(target);
        }
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
