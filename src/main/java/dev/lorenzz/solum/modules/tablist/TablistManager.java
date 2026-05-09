package dev.lorenzz.solum.modules.tablist;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Config;
import dev.lorenzz.solum.utils.Messages;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TablistManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('\u00A7')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private final Solum plugin;
    private final Config config;
    private final Set<String> ownedTeams = new HashSet<>();
    private final Map<String, Integer> weightCache = new HashMap<>();

    public enum SortType {
        GROUPS,
        GROUPS_THEN_NAME,
        WEIGHT,
        WEIGHT_THEN_NAME,
        NAME,
        NONE
    }

    private Scoreboard scoreboard;
    private BukkitTask updateTask;
    private SortType sortType = SortType.WEIGHT_THEN_NAME;
    private List<String> sortGroups = new ArrayList<>();

    public TablistManager(Solum plugin) {
        this.plugin = plugin;
        this.config = new Config(plugin, "tablist.yml");
    }

    public Config getConfig() { return config; }

    public void start() {
        if (!config.getBoolean("enabled", true)) {
            plugin.getLogger().info("[Solum] Tablist disabled in tablist.yml.");
            return;
        }
        loadSortConfig(true);
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Player p : Bukkit.getOnlinePlayers()) attach(p);

        long ticks = Math.max(20L, config.getLong("update-interval", 40L));
        this.updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, ticks, ticks);
        plugin.getLogger().info("[Solum] Tablist started (update every " + ticks + " ticks).");
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        Component empty = Component.empty();
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.sendPlayerListHeaderAndFooter(empty, empty); } catch (Throwable ignored) {}
        }

        if (scoreboard != null) {
            for (String name : new ArrayList<>(ownedTeams)) {
                Team t = scoreboard.getTeam(name);
                if (t != null) {
                    try { t.unregister(); } catch (Throwable ignored) {}
                }
            }
        }

        ownedTeams.clear();
        weightCache.clear();
        scoreboard = null;

    }

    public void attach(Player player) {
        applyHeaderFooter(player);
        applyTeamFor(player);
    }

    public void detach(Player player) {
        weightCache.remove(player.getName());
        if (scoreboard == null) return;
        for (String name : new ArrayList<>(ownedTeams)) {
            Team team = scoreboard.getTeam(name);
            if (team == null) { ownedTeams.remove(name); continue; }
            if (team.hasEntry(player.getName())) {
                try { team.removeEntry(player.getName()); } catch (Throwable ignored) {}
            }
            if (team.getEntries().isEmpty()) {
                try { team.unregister(); } catch (Throwable ignored) {}
                ownedTeams.remove(name);
            }
        }
    }

    public void refreshAll() {
        if (scoreboard == null) return;
        weightCache.clear();
        loadSortConfig();
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyHeaderFooter(p);
            applyTeamFor(p);
        }
    }

    private void applyHeaderFooter(Player player) {
        String design = config.getString("default-design", "default");
        List<String> headerLines = config.getStringList("designs." + design + ".header");
        List<String> footerLines = config.getStringList("designs." + design + ".footer");

        Component header = buildComponent(player, headerLines);
        Component footer = buildComponent(player, footerLines);

        try {
            player.sendPlayerListHeaderAndFooter(header, footer);
        } catch (Throwable t) {
            plugin.getLogger().fine("[Solum] sendPlayerListHeaderAndFooter failed: " + t.getMessage());
        }
    }

    private Component buildComponent(Player ctx, List<String> lines) {
        if (lines == null || lines.isEmpty()) return Component.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(applyPlaceholders(ctx, lines.get(i)));
        }
        return LEGACY.deserialize(Messages.color(sb.toString()));
    }

    private void applyTeamFor(Player player) {
        if (scoreboard == null) return;
        String teamName = teamNameFor(player);

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            try {
                team = scoreboard.registerNewTeam(teamName);
                ownedTeams.add(teamName);
            } catch (Throwable t) {
                return;
            }
        }

        String section = resolveGroupSection(player);
        String tabPrefix = applyPlaceholders(player,
                config.getString(section + ".tabprefix", ""));
        String tabSuffix = applyPlaceholders(player,
                config.getString(section + ".tabsuffix", ""));
        String custom = applyPlaceholders(player,
                config.getString(section + ".customtabname", "%player_name%"));
        String tagPrefix = applyPlaceholders(player,
                config.getString(section + ".tagprefix",
                        config.getString(section + ".tabprefix", "")));
        String tagSuffix = applyPlaceholders(player,
                config.getString(section + ".tagsuffix",
                        config.getString(section + ".tabsuffix", "")));

        try {
            team.prefix(LEGACY.deserialize(Messages.color(tagPrefix)));
            team.suffix(LEGACY.deserialize(Messages.color(tagSuffix)));
        } catch (NoSuchMethodError legacy) {
            try {
                team.setPrefix(truncate(Messages.color(tagPrefix), 64));
                team.setSuffix(truncate(Messages.color(tagSuffix), 64));
            } catch (Throwable ignored) {}
        }

        if (!team.hasEntry(player.getName())) {
            for (String other : new ArrayList<>(ownedTeams)) {
                if (other.equals(teamName)) continue;
                Team o = scoreboard.getTeam(other);
                if (o != null && o.hasEntry(player.getName())) {
                    try { o.removeEntry(player.getName()); } catch (Throwable ignored) {}
                    if (o.getEntries().isEmpty()) {
                        try { o.unregister(); } catch (Throwable ignored) {}
                        ownedTeams.remove(other);
                    }
                }
            }
            try { team.addEntry(player.getName()); } catch (Throwable ignored) {}
        }

        try {
            Component disp = LEGACY.deserialize(Messages.color(tabPrefix + custom + tabSuffix));
            player.playerListName(disp);
        } catch (Throwable ignored) {}
    }

    private String teamNameFor(Player player) {
        String key = sortKeyFor(player);
        String base = "z" + key + "_" + player.getName();
        return base.length() > 16 ? base.substring(0, 16) : base;
    }

    private String sortKeyFor(Player player) {
        switch (sortType) {
            case GROUPS:
            case GROUPS_THEN_NAME: {
                String group = getPrimaryGroup(player);
                int idx = sortGroups.indexOf(group == null ? "" : group.toLowerCase());
                if (idx < 0) idx = sortGroups.size();
                return String.format("%04d", Math.min(9999, idx));
            }
            case NAME:
                return "0000";
            case NONE:
                return "9999";
            case WEIGHT:
            case WEIGHT_THEN_NAME:
            default: {
                int w = Math.max(0, Math.min(9999, getWeight(player)));
                return String.format("%04d", w);
            }
        }
    }

    private void loadSortConfig() {
        loadSortConfig(false);
    }

    private void loadSortConfig(boolean verbose) {
        String raw = config.getString("sort-type", "WEIGHT_THEN_NAME");
        try {
            this.sortType = SortType.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("[Solum] Unknown tablist sort-type '" + raw
                    + "'. Falling back to WEIGHT_THEN_NAME. "
                    + "Valid: GROUPS, GROUPS_THEN_NAME, WEIGHT, WEIGHT_THEN_NAME, NAME, NONE.");
            this.sortType = SortType.WEIGHT_THEN_NAME;
        }
        List<String> raws = config.getStringList("sort-groups");
        this.sortGroups = new ArrayList<>(raws.size());
        for (String g : raws) if (g != null) this.sortGroups.add(g.toLowerCase());
        if (verbose) {
            plugin.getLogger().info("[Solum] Tablist sort-type=" + sortType
                    + (sortGroups.isEmpty() ? "" : " groups=" + sortGroups));
        }
    }

    private String resolveGroupSection(Player player) {
        String group = getPrimaryGroup(player);
        if (group != null && !group.isEmpty() && config.isConfigurationSection("groups." + group)) {
            return "groups." + group;
        }
        return "groups._DEFAULT_";
    }

    private String getPrimaryGroup(Player player) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return "default";
        try {
            RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (rsp == null) return "default";
            User user = rsp.getProvider().getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getPrimaryGroup() : "default";
        } catch (Throwable t) {
            return "default";
        }
    }

    private int getWeight(Player player) {
        Integer cached = weightCache.get(player.getName());
        if (cached != null) return cached;
        int w = config.getInt("groups." + getPrimaryGroup(player) + ".weight",
                config.getInt("groups._DEFAULT_.weight", 1000));
        weightCache.put(player.getName(), w);
        return w;
    }

    private String applyPlaceholders(Player ctx, String input) {
        if (input == null || input.isEmpty()) return "";
        String s = input
                .replace("%player%", ctx.getName())
                .replace("%player_name%", ctx.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                s = PlaceholderAPI.setPlaceholders(ctx, s);
            } catch (Throwable ignored) {}
        }
        return s;
    }

    @SuppressWarnings("SameParameterValue")
    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    @SuppressWarnings("unused")
    public List<Player> sortedPlayers() {
        List<Player> list = new ArrayList<>(Bukkit.getOnlinePlayers());
        Comparator<Player> cmp;
        switch (sortType) {
            case GROUPS:
                cmp = Comparator.comparingInt(this::groupIndex);
                break;
            case GROUPS_THEN_NAME:
                cmp = Comparator.comparingInt(this::groupIndex)
                        .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case WEIGHT:
                cmp = Comparator.comparingInt(this::getWeight);
                break;
            case NAME:
                cmp = Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case NONE:
                return list;
            case WEIGHT_THEN_NAME:
            default:
                cmp = Comparator.comparingInt(this::getWeight)
                        .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER);
        }
        list.sort(cmp);
        return list;
    }

    private int groupIndex(Player player) {
        String group = getPrimaryGroup(player);
        int idx = sortGroups.indexOf(group == null ? "" : group.toLowerCase());
        return idx < 0 ? Integer.MAX_VALUE : idx;
    }
}

