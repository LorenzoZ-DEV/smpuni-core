package dev.lorenzz.solum.modules.nametag;

import dev.lorenzz.solum.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('§')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static final Color BACKGROUND_COLOR = Color.fromARGB(0, 0, 0, 0);
    private static final boolean TEXT_SHADOW = true;
    private static final boolean SEE_THROUGH_BLOCKS = false;

    private static final String HIDE_TEAM = "mnu_hidetag";

    private final Plugin plugin;
    private final NamespacedKey markerKey;
    private final Map<UUID, DisplayEntry> displays = new ConcurrentHashMap<>();

    private static final class DisplayEntry {
        final TextDisplay entity;
        double lastX = Double.NaN;
        double lastY = Double.NaN;
        double lastZ = Double.NaN;

        DisplayEntry(TextDisplay entity) {
            this.entity = entity;
        }
    }

    private FileConfiguration config;
    private boolean enabled = true;
    private boolean hideVanillaNametag = false;
    private boolean hideFromOwner = true;
    private float viewRange = 2.0f;
    private float offsetY = 0.35f;
    private int textRefreshTicks = 20;
    private List<String> lines = List.of("&f%player_name%");

    private BukkitTask refreshTask;
    private BukkitTask followTask;

    public NametagManager(Plugin plugin) {
        this.plugin = plugin;
        this.markerKey = new NamespacedKey(plugin, "nametag_display");
        reloadConfig();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void reloadConfig() {
        File file = new File(plugin.getDataFolder(), "nametag.yml");
        if (!file.exists()) {
            plugin.saveResource("nametag.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader def = new InputStreamReader(
                plugin.getResource("nametag.yml"), StandardCharsets.UTF_8)) {
            this.config.setDefaults(YamlConfiguration.loadConfiguration(def));
        } catch (Exception ignored) {
        }
        this.enabled = config.getBoolean("enabled");
        this.hideVanillaNametag = config.getBoolean("hide-vanilla-nametag");
        this.hideFromOwner = config.getBoolean("hide-from-owner");
        this.viewRange = (float) config.getDouble("view-range");
        this.offsetY = (float) config.getDouble("offset-y");
        this.textRefreshTicks = Math.max(1, config.getInt("text-refresh-ticks", 20));
        List<String> raw = config.getStringList("lines");
        this.lines = raw.isEmpty() ? List.of("&f%player_name%") : raw;
    }

    public void start() {
        cleanupOrphans();
        if (!enabled) return;
        ensureHideTeam();
        for (Player p : Bukkit.getOnlinePlayers()) attach(p);
        startTextRefreshTask();
        startFollowTask();
    }

    public void stop() {
        stopTextRefreshTask();
        stopFollowTask();
        for (DisplayEntry entry : displays.values()) {
            TextDisplay td = entry.entity;
            if (td != null && !td.isDead()) td.remove();
        }
        displays.clear();
        removeHideTeam();
        cleanupOrphans();
    }

    public void reload() {
        stop();
        reloadConfig();
        start();
    }

    private Team ensureHideTeam() {
        if (!hideVanillaNametag) return null;
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = sb.getTeam(HIDE_TEAM);
        if (team == null) {
            try {
                team = sb.registerNewTeam(HIDE_TEAM);
            } catch (IllegalArgumentException ex) {
                team = sb.getTeam(HIDE_TEAM);
            }
        }
        if (team != null) {
            try {
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            } catch (Throwable ignored) {
            }
        }
        return team;
    }

    private void removeHideTeam() {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = sb.getTeam(HIDE_TEAM);
        if (team != null) {
            try {
                team.unregister();
            } catch (Throwable ignored) {
            }
        }
    }

    private void addToHideTeam(Player player) {
        if (!hideVanillaNametag) return;
        Team team = ensureHideTeam();
        if (team == null) return;
        if (!team.hasEntry(player.getName())) {
            try {
                team.addEntry(player.getName());
            } catch (Throwable ignored) {
            }
        }
    }

    public void attach(Player player) {
        if (!enabled) return;
        if (player == null || !player.isOnline() || player.isDead()) return;

        removeExistingTagsOn(player);
        addToHideTeam(player);

        Location spawn = headAnchor(player);

        TextDisplay td = player.getWorld().spawn(spawn, TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setSeeThrough(SEE_THROUGH_BLOCKS);
            t.setShadowed(TEXT_SHADOW);
            t.setDefaultBackground(false);
            t.setBackgroundColor(BACKGROUND_COLOR);
            t.setPersistent(false);
            t.setViewRange(viewRange);
            t.setInterpolationDuration(0);
            try {
                t.setTeleportDuration(3);
            } catch (Throwable ignored) {
            }

            Transformation tr = t.getTransformation();
            t.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    tr.getLeftRotation(),
                    tr.getScale(),
                    tr.getRightRotation()
            ));
            t.getPersistentDataContainer().set(markerKey, PersistentDataType.STRING,
                    player.getUniqueId().toString());
            t.text(buildComponent(player));
        });

        if (hideFromOwner) {
            try {
                player.hideEntity(plugin, td);
            } catch (Throwable ignored) {
            }
        }

        DisplayEntry entry = new DisplayEntry(td);
        Location loc = player.getLocation();
        entry.lastX = loc.getX();
        entry.lastY = loc.getY();
        entry.lastZ = loc.getZ();
        displays.put(player.getUniqueId(), entry);
    }

    private Location headAnchor(Player player) {
        Location loc = player.getLocation();
        loc.setY(loc.getY() + 1.8 + offsetY);
        loc.setPitch(0f);
        loc.setYaw(0f);
        return loc;
    }

    public void detach(Player player) {
        if (player == null) return;
        DisplayEntry entry = displays.remove(player.getUniqueId());
        if (entry != null && entry.entity != null && !entry.entity.isDead()) {
            entry.entity.remove();
        }
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = sb.getTeam(HIDE_TEAM);
        if (team != null && team.hasEntry(player.getName())) {
            try {
                team.removeEntry(player.getName());
            } catch (Throwable ignored) {
            }
        }
    }

    public void refresh(Player player) {
        if (!enabled) return;
        if (player == null || !player.isOnline() || player.isDead()) return;
        DisplayEntry entry = displays.get(player.getUniqueId());
        TextDisplay td = entry == null ? null : entry.entity;
        if (td == null || td.isDead()) {
            if (entry != null) displays.remove(player.getUniqueId());
            attach(player);
            return;
        }
        td.text(buildComponent(player));
    }

    public void updatePosition(Player player) {
        if (!enabled) return;
        if (player == null || !player.isOnline() || player.isDead()) return;
        DisplayEntry entry = displays.get(player.getUniqueId());
        if (entry == null) {
            attach(player);
            return;
        }
        TextDisplay td = entry.entity;
        if (td == null || td.isDead() || !td.getWorld().equals(player.getWorld())) {
            if (td != null && !td.isDead()) td.remove();
            displays.remove(player.getUniqueId());
            attach(player);
            return;
        }
        try {
            td.teleport(headAnchor(player));
            Location loc = player.getLocation();
            entry.lastX = loc.getX();
            entry.lastY = loc.getY();
            entry.lastZ = loc.getZ();
        } catch (Throwable ignored) {
        }
    }

    private void startTextRefreshTask() {
        stopTextRefreshTask();
        long period = Math.max(1, textRefreshTicks);
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllText, period, period);
    }

    private void stopTextRefreshTask() {
        if (refreshTask != null) {
            try { refreshTask.cancel(); } catch (Throwable ignored) {}
            refreshTask = null;
        }
    }

    private void startFollowTask() {
        stopFollowTask();
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::followAll, 1L, 1L);
    }

    private void stopFollowTask() {
        if (followTask != null) {
            try { followTask.cancel(); } catch (Throwable ignored) {}
            followTask = null;
        }
    }

    private void followAll() {
        if (displays.isEmpty()) return;
        var it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DisplayEntry> mapEntry = it.next();
            Player player = Bukkit.getPlayer(mapEntry.getKey());
            if (player == null || !player.isOnline()) continue;
            if (player.isDead()) continue;

            DisplayEntry entry = mapEntry.getValue();
            TextDisplay td = entry.entity;
            if (td == null || td.isDead()) {
                it.remove();
                continue;
            }
            if (!td.getWorld().equals(player.getWorld())) {
                td.remove();
                it.remove();
                attach(player);
                continue;
            }

            Location loc = player.getLocation();
            double x = loc.getX(), y = loc.getY(), z = loc.getZ();
            if (x == entry.lastX && y == entry.lastY && z == entry.lastZ) {
                continue;
            }
            entry.lastX = x;
            entry.lastY = y;
            entry.lastZ = z;

            try {
                td.teleport(headAnchor(player));
            } catch (Throwable ignored) {
            }
        }
    }

    private void refreshAllText() {
        if (displays.isEmpty()) return;
        var it = displays.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DisplayEntry> mapEntry = it.next();
            Player player = Bukkit.getPlayer(mapEntry.getKey());
            DisplayEntry entry = mapEntry.getValue();
            TextDisplay td = entry.entity;
            if (player == null || !player.isOnline()) {
                if (td != null && !td.isDead()) td.remove();
                it.remove();
                continue;
            }
            if (player.isDead()) continue;
            if (td == null || td.isDead()) {
                it.remove();
                continue;
            }
            td.text(buildComponent(player));
        }
    }

    private Component buildComponent(Player p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(applyPlaceholders(p, lines.get(i)));
        }
        return LEGACY.deserialize(Messages.color(sb.toString()));
    }

    private String applyPlaceholders(Player p, String input) {
        if (input == null || input.isEmpty()) return "";
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, input);
            } catch (Throwable ignored) {
            }
        }
        return input;
    }

    private void cleanupOrphans() {
        for (org.bukkit.World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntitiesByClass(TextDisplay.class)) {
                if (e.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)) {
                    try { e.leaveVehicle(); } catch (Throwable ignored) {}
                    e.remove();
                }
            }
        }
    }

    private void removeExistingTagsOn(Player player) {
        DisplayEntry prev = displays.remove(player.getUniqueId());
        if (prev != null && prev.entity != null && !prev.entity.isDead()) {
            prev.entity.remove();
        }
        try {
            for (Entity passenger : player.getPassengers()) {
                if (passenger instanceof TextDisplay td
                        && td.getPersistentDataContainer().has(markerKey, PersistentDataType.STRING)) {
                    try { player.removePassenger(td); } catch (Throwable ignored) {}
                    td.remove();
                }
            }
        } catch (Throwable ignored) {}
    }
}

