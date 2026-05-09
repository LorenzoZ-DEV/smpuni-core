package dev.lorenzz.solum.modules.nametag;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
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

public class NametagManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('\u00A7')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static final double STATS_OFFSET_ABOVE_HEAD = 0.5;
    private static final double LINE_HEIGHT = 0.28;
    private static final long UPDATE_INTERVAL_TICKS = 20L;
    private static final boolean HIDE_FROM_OWNER = true;
    private static final Color BACKGROUND_COLOR = Color.fromARGB(0, 0, 0, 0);
    private static final boolean TEXT_SHADOW = true;
    private static final boolean SEE_THROUGH_BLOCKS = false;
    private static final String NICKNAME_FORMAT = "%luckperms_prefix%%player_name%";
    private static final String STATS_FORMAT =
            "&e\uD83E\uDE99 %solum_shards% &7|| &c\u2764 %player_health_rounded%%solum_team_tag_sep%";

    private final Solum plugin;
    private final NamespacedKey markerKey;
    private final Map<UUID, UUID> displays = new ConcurrentHashMap<>();

    private BukkitTask updateTask;

    public NametagManager(Solum plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "nametag_display");
    }

    public void start() {
        cleanupOrphans();
        for (Player p : Bukkit.getOnlinePlayers()) attach(p);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll,
                UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);
        plugin.getLogger().info("[Solum] NametagManager started (statsOffset="
                + STATS_OFFSET_ABOVE_HEAD + ", lineHeight=" + LINE_HEIGHT + ").");
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

    private double computeAnchorY(Player p) {
        return p.getHeight() + STATS_OFFSET_ABOVE_HEAD + LINE_HEIGHT;
    }

    public void attach(Player player) {
        if (player == null || !player.isOnline()) return;
        if (player.isDead()) return;
        if (displays.containsKey(player.getUniqueId())) return;

        Location loc = player.getLocation();
        final float anchor = (float) computeAnchorY(player);

        TextDisplay td = player.getWorld().spawn(loc, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setSeeThrough(SEE_THROUGH_BLOCKS);
            t.setShadowed(TEXT_SHADOW);
            t.setDefaultBackground(false);
            t.setBackgroundColor(BACKGROUND_COLOR);
            t.setPersistent(false);
            t.setViewRange(1.0f);
            Transformation tr = t.getTransformation();
            t.setTransformation(new Transformation(
                    new Vector3f(0f, anchor, 0f),
                    tr.getLeftRotation(),
                    tr.getScale(),
                    tr.getRightRotation()
            ));
            t.setInterpolationDuration(0);
            t.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING,
                    player.getUniqueId().toString());
            t.text(buildComponent(player));
        });

        boolean mounted = false;
        try { mounted = player.addPassenger(td); } catch (Throwable ignored) {}
        if (!mounted) {
            plugin.getLogger().warning("[Solum] Failed to mount nametag display on "
                    + player.getName());
            td.remove();
            return;
        }

        if (HIDE_FROM_OWNER) {
            try { player.hideEntity(plugin, td); } catch (Throwable ignored) {}
        }

        displays.put(player.getUniqueId(), td.getUniqueId());
    }

    public void detach(Player player) {
        if (player == null) return;
        UUID id = displays.remove(player.getUniqueId());
        if (id == null) return;
        Entity e = Bukkit.getEntity(id);
        if (e != null) {
            try { e.leaveVehicle(); } catch (Throwable ignored) {}
            e.remove();
        }
    }

    private void updateAll() {
        Iterator<Map.Entry<UUID, UUID>> it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline() || p.isDead()) {
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
            if (td.getVehicle() == null || !td.getVehicle().equals(p)) {
                td.remove();
                it.remove();
                attach(p);
                continue;
            }
            td.text(buildComponent(p));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead()) continue;
            if (!displays.containsKey(p.getUniqueId())) attach(p);
        }
    }

    private Component buildComponent(Player p) {
        String nickname = NICKNAME_FORMAT;
        String stats = STATS_FORMAT;
        nickname = nickname.replace("%player%", p.getName())
                           .replace("%player_name%", p.getName());
        stats = stats.replace("%player%", p.getName())
                     .replace("%player_name%", p.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                nickname = PlaceholderAPI.setPlaceholders(p, nickname);
                stats = PlaceholderAPI.setPlaceholders(p, stats);
            } catch (Throwable ignored) {}
        }
        String combined = Messages.color(nickname) + "\n" + Messages.color(stats);
        return LEGACY.deserialize(combined);
    }

    private void cleanupOrphans() {
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntitiesByClass(TextDisplay.class)) {
                if (e.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)) {
                    e.remove();
                }
            }
        }
    }

    public void refresh(Player player) {
        if (player == null) return;
        UUID id = displays.get(player.getUniqueId());
        if (id == null) return;
        Entity e = Bukkit.getEntity(id);
        if (!(e instanceof TextDisplay td) || td.isDead()) return;

        float anchor = (float) computeAnchorY(player);
        Transformation tr = td.getTransformation();
        Vector3f cur = tr.getTranslation();
        if (Math.abs(cur.y - anchor) > 0.001f) {
            td.setTransformation(new Transformation(
                    new Vector3f(0f, anchor, 0f),
                    tr.getLeftRotation(), tr.getScale(), tr.getRightRotation()
            ));
        }
        td.text(buildComponent(player));
    }
}
