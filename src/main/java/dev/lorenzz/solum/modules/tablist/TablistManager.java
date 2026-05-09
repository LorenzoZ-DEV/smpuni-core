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

/**
 * Tab list manager (NEZNAMY/TAB style).
 * <ul>
 *   <li>Header / footer applied via Paper's Adventure player list API.</li>
 *   <li>Per-player prefix / suffix via Bukkit Scoreboard Teams, sorted by
 *       a "weight" pulled from the LuckPerms primary group.</li>
 * </ul>
 * Because we only touch real players (no fake 80-slot grid) the vanilla
 * Minecraft client decides how many columns to display based on the player
 * count: 1-20 → 1 col, 21-40 → 2 cols, 41-60 → 3 cols, 61-80 → 4 cols.
 *
 * @see <a href="https://github.com/NEZNAMY/TAB">NEZNAMY/TAB</a>
 */
public class TablistManager {

    /** Legacy → Component serializer (supports {@code §} + Bungee hex). */
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('\u00A7')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private final Solum plugin;
    private final Config config;
    /** Track team names we created so we can clean up. */
    private final Set<String> ownedTeams = new HashSet<>();
    /** Cached weights so the comparator is deterministic mid-sort. */
    private final Map<String, Integer> weightCache = new HashMap<>();

    /** Supported sort strategies (NEZNAMY/TAB compatible names). */
    public enum SortType {
        /** Order strictly by the {@code sort-groups} list (then by name). */
        GROUPS,
        /** Like {@link #GROUPS} but explicit fallback to alphabetical. */
        GROUPS_THEN_NAME,
        /** Order by per-group {@code weight} (lower = higher in list). */
        WEIGHT,
        /** Like {@link #WEIGHT} with alphabetical tiebreak. */
        WEIGHT_THEN_NAME,
        /** Alphabetical only. */
        NAME,
        /** Disable Solum sorting (clients keep server insertion order). */
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

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    public void start() {
        if (!config.getBoolean("enabled", true)) {
            plugin.getLogger().info("[Solum] Tablist disabled in tablist.yml.");
            return;
        }
        loadSortConfig(true);
        // Use the main scoreboard so other plugins reading from it (e.g.
        // sidebar managers) keep working alongside our teams.
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Initial pass over already-online players (handy on /reload).
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

    // ---------------------------------------------------------------------
    // Hooks called by TablistListener
    // ---------------------------------------------------------------------

    public void attach(Player player) {
        applyHeaderFooter(player);
        applyTeamFor(player);
    }

    public void detach(Player player) {
        weightCache.remove(player.getName());
        if (scoreboard == null) return;
        // Find any owned team that still references this player and clean up.
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

    // ---------------------------------------------------------------------
    // Header / Footer
    // ---------------------------------------------------------------------

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

    // ---------------------------------------------------------------------
    // Team prefix / suffix + display name
    // ---------------------------------------------------------------------

    /**
     * Build (or update) a Scoreboard Team for the given player. The team
     * name is prefixed with a 4-digit zero-padded weight so vanilla MC
     * sorts the tab list by group rank automatically.
     */
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
        // Nametag (above head + chat): fall back to tab values if not configured.
        String tagPrefix = applyPlaceholders(player,
                config.getString(section + ".tagprefix",
                        config.getString(section + ".tabprefix", "")));
        String tagSuffix = applyPlaceholders(player,
                config.getString(section + ".tagsuffix",
                        config.getString(section + ".tabsuffix", "")));

        // The Scoreboard Team prefix/suffix controls the NAMETAG above the
        // player's head (and chat name in vanilla). Use tagprefix/tagsuffix.
        try {
            team.prefix(LEGACY.deserialize(Messages.color(tagPrefix)));
            team.suffix(LEGACY.deserialize(Messages.color(tagSuffix)));
        } catch (NoSuchMethodError legacy) {
            try {
                team.setPrefix(truncate(Messages.color(tagPrefix), 64));
                team.setSuffix(truncate(Messages.color(tagSuffix), 64));
            } catch (Throwable ignored) {}
        }

        // Make sure THIS player is the only entry in this team and remove
        // them from any other Solum team they may belong to.
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

        // Customise the player-list display name (the actual cell content).
        try {
            Component disp = LEGACY.deserialize(Messages.color(tabPrefix + custom + tabSuffix));
            player.playerListName(disp);
        } catch (Throwable ignored) {}
    }

    /**
     * Team name = "z" + 4-char sort key (depending on {@link SortType}) +
     * "_" + first chars of player name, capped at 16 characters (Bukkit
     * legacy limit). Minecraft sorts the tab list alphabetically by team
     * name, so a deterministic prefix means a deterministic order.
     */
    private String teamNameFor(Player player) {
        String key = sortKeyFor(player);
        String base = "z" + key + "_" + player.getName();
        return base.length() > 16 ? base.substring(0, 16) : base;
    }

    /** 4-character sort key used as the team-name prefix. */
    private String sortKeyFor(Player player) {
        switch (sortType) {
            case GROUPS:
            case GROUPS_THEN_NAME: {
                String group = getPrimaryGroup(player);
                int idx = sortGroups.indexOf(group == null ? "" : group.toLowerCase());
                if (idx < 0) idx = sortGroups.size(); // unknown groups go last
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

    /** Read {@code sort-type} + {@code sort-groups} from tablist.yml. */
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

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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

