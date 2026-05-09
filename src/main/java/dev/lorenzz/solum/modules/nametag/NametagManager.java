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
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

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

    private static final double STATS_OFFSET_ABOVE_HEAD = 0.1;
    private static final double LINE_HEIGHT = 0.25;
    private static final boolean HIDE_FROM_OWNER = true;
    private static final Color BACKGROUND_COLOR = Color.fromARGB(0, 0, 0, 0);
    private static final boolean TEXT_SHADOW = true;
    private static final boolean SEE_THROUGH_BLOCKS = false;
    private static final String NICKNAME_FORMAT = "%luckperms_prefix%%player_name%";
    private static final String STATS_FORMAT =
            "&e\uD83E\uDE99 %solum_shards% &7|| &c\u2764 %player_health_rounded%%solum_team_tag_sep%";

    private static volatile NametagManager INSTANCE;

    private final Solum plugin;
    private final NamespacedKey markerKey;
    private final Map<UUID, UUID> displays = new ConcurrentHashMap<>();

    public NametagManager(Solum plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "nametag_display");
    }

    public void start() {
        cleanupOrphans();
        for (Player p : Bukkit.getOnlinePlayers()) attach(p);
        INSTANCE = this;
    }

    public void stop() {
        if (INSTANCE == this) INSTANCE = null;
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

    public static void requestRefresh(Player player) {
        NametagManager m = INSTANCE;
        if (m != null && player != null) m.refresh(player);
    }

    private double computeAnchorY(Player p) {
        return STATS_OFFSET_ABOVE_HEAD + LINE_HEIGHT;
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
        if (player == null || !player.isOnline() || player.isDead()) return;
        UUID id = displays.get(player.getUniqueId());
        Entity e = (id == null) ? null : Bukkit.getEntity(id);
        if (!(e instanceof TextDisplay td) || td.isDead()
                || td.getVehicle() == null || !td.getVehicle().equals(player)) {

            if (id != null) displays.remove(player.getUniqueId());
            if (e != null) e.remove();
            attach(player);
            return;
        }

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
