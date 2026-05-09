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
    private final Map<UUID, UUID> displays = new ConcurrentHashMap<>();

    private BukkitTask updateTask;

    private double extraAboveEyes = -0.07;
    private long updateIntervalTicks = 20L;
    @SuppressWarnings("unused")
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

        plugin.getLogger().info("[Solum] NametagManager started (extra-above-eyes="
                + extraAboveEyes + ", mode=passenger).");
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
        if (cfg.isSet("nametag.extra-above-eyes")) {
            this.extraAboveEyes = cfg.getDouble("nametag.extra-above-eyes", -0.07);
        } else if (cfg.isSet("nametag.y-offset")) {
            this.extraAboveEyes = cfg.getDouble("nametag.y-offset", 1.60) - 1.62;
        } else {
            this.extraAboveEyes = -0.07;
        }
        this.updateIntervalTicks = Math.max(1L, cfg.getLong("nametag.update-interval-ticks", 20L));
        this.teleportDuration = Math.max(0, cfg.getInt("nametag.teleport-duration", 3));
        this.format = cfg.getString("nametag.format", DEFAULT_FORMAT);
        this.hideFromOwner = cfg.getBoolean("nametag.hide-from-owner", true);
    }

    public void setExtraAboveEyes(double value) {
        this.extraAboveEyes = value;
        plugin.getConfig().set("nametag.extra-above-eyes", value);
        plugin.saveConfig();
    }

    public double getExtraAboveEyes() { return extraAboveEyes; }

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

    private double computeAnchorY(Player p) {
        return p.getEyeHeight() + extraAboveEyes;
    }

    public void attach(Player player) {
        if (player == null || !player.isOnline()) return;
        if (player.isDead()) return;
        if (displays.containsKey(player.getUniqueId())) return;

        Location loc = player.getLocation();
        final float anchor = (float) computeAnchorY(player);

        TextDisplay td = player.getWorld().spawn(loc, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setSeeThrough(false);
            t.setShadowed(false);
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
        String s = format == null ? "" : format;
        s = s.replace("%player%", p.getName())
             .replace("%player_name%", p.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try { s = PlaceholderAPI.setPlaceholders(p, s); } catch (Throwable ignored) {}
        }
        return LEGACY.deserialize(Messages.color(s));
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
}
