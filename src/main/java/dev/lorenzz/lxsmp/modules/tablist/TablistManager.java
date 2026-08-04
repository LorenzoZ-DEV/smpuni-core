package dev.lorenzz.lxsmp.modules.tablist;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import dev.lorenzz.lxsmp.utils.LuckPermsBridge;
import dev.lorenzz.lxsmp.utils.PlaceholderCache;
import dev.lorenzz.lxsmp.utils.Placeholders;
import dev.lorenzz.lxsmp.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class TablistManager {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder()
                    .character('\u00A7')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    public enum SortType {
        GROUPS,
        GROUPS_THEN_NAME,
        WEIGHT,
        WEIGHT_THEN_NAME,
        NAME,
        NONE
    }

    private final Plugin plugin;
    private final Set<String> ownedTeams = new HashSet<>();

    private final Cache<UUID, TabPlayerData> playerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(10_000)
            .build();

    private static final class AppliedState {
        String teamName;
        String prefixRaw;
        String suffixRaw;
        String listNameRaw;
        String headerRaw;
        String footerRaw;
        boolean teamOptionsApplied;
    }

    private final Cache<UUID, AppliedState> appliedCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(10_000)
            .build();

    private static final class AppliedTeamState {

        String prefixRaw;
        String suffixRaw;
        boolean optionsApplied;
    }

    private final Map<String, AppliedTeamState> teamStates = new HashMap<>();

    private static final int COMPONENT_CACHE_CAP = 1024;
    private final java.util.concurrent.ConcurrentHashMap<String, Component> componentCache
            = new java.util.concurrent.ConcurrentHashMap<>(256);

    private PlaceholderCache placeholderCache;

    private FileConfiguration config;
    private Scoreboard scoreboard;
    private BukkitTask updateTask;
    private SortType sortType = SortType.WEIGHT_THEN_NAME;
    private List<String> sortGroups = new ArrayList<>();

    private long updateInterval = 40L;
    private int chunkCursor = 0;

    private static final class GroupTpls {
        final String tabPrefix;
        final String tabSuffix;
        final String tagPrefix;
        final String tagSuffix;
        GroupTpls(String tp, String ts, String gp, String gs) {
            this.tabPrefix = tp; this.tabSuffix = ts;
            this.tagPrefix = gp; this.tagSuffix = gs;
        }
    }
    private volatile String tplHeader = "";
    private volatile String tplFooter = "";
    private volatile Map<String, GroupTpls> tplGroups = Collections.emptyMap();
    private volatile GroupTpls tplDefaultGroup = new GroupTpls("", "", "", "");
    private volatile boolean enabled = true;

    private Player[] snapPlayers = new Player[0];
    private String[] snapGroups = new String[0];
    private int[] snapWeights = new int[0];
    private int snapSize = 0;

    public TablistManager(Plugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void reloadConfig() {
        File file = new File(plugin.getDataFolder(), "tablist.yml");
        if (!file.exists()) {
            plugin.saveResource("tablist.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader def = new InputStreamReader(
                plugin.getResource("tablist.yml"), StandardCharsets.UTF_8)) {

            this.config.setDefaults(YamlConfiguration.loadConfiguration(def));

        } catch (Exception ignored) {
        }

        long ttlMs = Math.max(0L, config.getLong("placeholder-cache-ttl-ms"));
        this.placeholderCache = (ttlMs == 0L) ? null : new PlaceholderCache(ttlMs);

        this.enabled = config.getBoolean("enabled", true);
        this.updateInterval = Math.max(20L, config.getLong("update-interval"));
        String design = config.getString("default-design", "default");
        this.tplHeader = joinLines(config.getStringList("designs." + design + ".header"));
        this.tplFooter = joinLines(config.getStringList("designs." + design + ".footer"));

        Map<String, GroupTpls> map = new HashMap<>();
        var groupsSection = config.getConfigurationSection("groups");
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                String base = "groups." + key + ".";
                GroupTpls g = new GroupTpls(
                        config.getString(base + "tabprefix", ""),
                        config.getString(base + "tabsuffix", ""),
                        config.getString(base + "tagprefix", ""),
                        config.getString(base + "tagsuffix", ""));
                map.put(key.toLowerCase(), g);
                if (key.equalsIgnoreCase("_DEFAULT_")) {
                    this.tplDefaultGroup = g;
                }
            }
        }
        this.tplGroups = Map.copyOf(map);
    }

    public void start() {
        if (!enabled) return;
        loadSortConfig();
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Player[] online = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (int i = 0; i < online.length; i++) {
            final Player ref = online[i];
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> { if (ref.isOnline()) attach(ref); }, 1L + (i / 4));
        }
        this.updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::refreshChunk, 1L, 1L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        Component empty = Component.empty();
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.sendPlayerListHeaderAndFooter(empty, empty);
            } catch (Throwable ignored) {
            }
        }
        if (scoreboard != null) {
            for (String name : new ArrayList<>(ownedTeams)) {
                Team t = scoreboard.getTeam(name);
                if (t != null) {
                    try {
                        t.unregister();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        ownedTeams.clear();
        playerCache.invalidateAll();
        appliedCache.invalidateAll();
        teamStates.clear();
        componentCache.clear();
        if (placeholderCache != null) placeholderCache.clear();
        scoreboard = null;
    }

    public void reload() {
        stop();
        reloadConfig();
        start();
    }

    public void attach(Player player) {
        if (scoreboard == null || player == null) return;
        final String headerTpl = tplHeader;
        final String footerTpl = tplFooter;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!player.isOnline()) return;
            final ResolvedTab r;
            try {
                TabPlayerData d = dataFor(player);
                String group = d.primaryGroup();
                int weight = d.weight();
                GroupTpls tpl = groupTplsFor(group);
                r = new ResolvedTab(
                        player.getUniqueId(),
                        player,
                        group,
                        weight,
                        applyPlaceholders(player, headerTpl),
                        applyPlaceholders(player, footerTpl),
                        applyPlaceholders(player, tpl.tabPrefix),
                        applyPlaceholders(player, tpl.tabSuffix),
                        applyPlaceholders(player, tpl.tagPrefix),
                        applyPlaceholders(player, tpl.tagSuffix));
            } catch (Throwable t) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || scoreboard == null) return;
                try {
                    applyResolvedFor(player, r);
                } catch (Throwable ignored) {}
            });
        });
    }

    public void detach(Player player) {
        if (scoreboard == null) return;
        for (String name : new ArrayList<>(ownedTeams)) {
            Team team = scoreboard.getTeam(name);
            if (team == null) {
                ownedTeams.remove(name);
                continue;
            }
            if (team.hasEntry(player.getName())) {
                try {
                    team.removeEntry(player.getName());
                } catch (Throwable ignored) {
                }
            }
            if (team.getEntries().isEmpty()) {
                try {
                    team.unregister();
                } catch (Throwable ignored) {
                }
                ownedTeams.remove(name);
                teamStates.remove(name);
                appliedCache.asMap().values().forEach(st -> {
                    if (name.equals(st.teamName)) {
                        st.teamName = null;
                        st.prefixRaw = null;
                        st.suffixRaw = null;
                        st.listNameRaw = null;
                        st.teamOptionsApplied = false;
                    }
                });
            }
        }
    }

    public void refreshAll() {
        if (scoreboard == null) return;
        final String headerTpl = tplHeader;
        final String footerTpl = tplFooter;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null || !p.isOnline()) continue;
            try {
                applyForPlayer(p, headerTpl, footerTpl);
            } catch (Throwable t) {
                plugin.getLogger().warning("Tablist refresh failed for "
                        + p.getName() + ": " + t);
            }
        }
    }

    private static final class ResolvedTab {
        final UUID uuid;
        final Player player;
        final String group;
        final int weight;
        final String header;
        final String footer;
        final String tabPrefix;
        final String tabSuffix;
        final String tagPrefix;
        final String tagSuffix;

        ResolvedTab(UUID uuid, Player player, String group, int weight,
                    String header, String footer,
                    String tabPrefix, String tabSuffix,
                    String tagPrefix, String tagSuffix) {
            this.uuid = uuid;
            this.player = player;
            this.group = group;
            this.weight = weight;
            this.header = header;
            this.footer = footer;
            this.tabPrefix = tabPrefix;
            this.tabSuffix = tabSuffix;
            this.tagPrefix = tagPrefix;
            this.tagSuffix = tagSuffix;
        }
    }

    private void refreshChunk() {
        if (scoreboard == null) return;
        Collection<? extends Player> onlineView = Bukkit.getOnlinePlayers();
        int total = onlineView.size();
        if (total == 0) {
            chunkCursor = 0;
            return;
        }

        int perTick = Math.max(1, (int) ((total + updateInterval - 1) / updateInterval));
        perTick = Math.min(perTick, 16);

        Player[] arr = onlineView.toArray(new Player[0]);
        if (chunkCursor >= arr.length) chunkCursor = 0;

        final String headerTpl = tplHeader;
        final String footerTpl = tplFooter;

        int end = Math.min(chunkCursor + perTick, arr.length);

        final int sliceLen = end - chunkCursor;
        final Player[] slicePlayers = new Player[sliceLen];
        int kept = 0;
        for (int i = chunkCursor; i < end; i++) {
            Player p = arr[i];
            if (p == null || !p.isOnline()) continue;
            slicePlayers[kept++] = p;
        }
        chunkCursor = (end >= arr.length) ? 0 : end;
        if (kept == 0) return;

        final int sliceSize = kept;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final ResolvedTab[] resolved = new ResolvedTab[sliceSize];
            for (int i = 0; i < sliceSize; i++) {
                Player p = slicePlayers[i];
                if (p == null || !p.isOnline()) continue;
                try {
                    TabPlayerData d = dataFor(p);
                    String group = d.primaryGroup();
                    int weight = d.weight();
                    GroupTpls tpl = groupTplsFor(group);

                    resolved[i] = new ResolvedTab(
                            p.getUniqueId(),
                            p,
                            group,
                            weight,
                            applyPlaceholders(p, headerTpl),
                            applyPlaceholders(p, footerTpl),
                            applyPlaceholders(p, tpl.tabPrefix),
                            applyPlaceholders(p, tpl.tabSuffix),
                            applyPlaceholders(p, tpl.tagPrefix),
                            applyPlaceholders(p, tpl.tagSuffix));
                } catch (Throwable t) {
                    resolved[i] = null;
                }
            }

            Bukkit.getScheduler().runTask(plugin, () -> applyResolvedTab(resolved, sliceSize));
        });
    }

    private void applyResolvedTab(ResolvedTab[] resolved, int len) {
        if (scoreboard == null) return;
        for (int i = 0; i < len; i++) {
            ResolvedTab r = resolved[i];
            if (r == null) continue;
            Player p = r.player;
            if (p == null || !p.isOnline()) continue;
            try {
                applyResolvedFor(p, r);
            } catch (Throwable t) {
            }
        }
    }

    private void applyResolvedFor(Player p, ResolvedTab r) {
        if (scoreboard == null) return;
        final UUID uuid = r.uuid;
        final String playerName = p.getName();

        final String headerResolved = r.header;
        final String footerResolved = r.footer;
        final String tabPrefix = r.tabPrefix;
        final String tabSuffix = r.tabSuffix;
        final String tagPrefix = r.tagPrefix;
        final String tagSuffix = r.tagSuffix;
        final String listNameRaw = tabPrefix + playerName + tabSuffix;
        final String teamName = teamNameFor(p, r.group, r.weight);

        final AppliedState st = appliedStateOf(uuid);
        final boolean headerFooterChanged = !headerResolved.equals(st.headerRaw)
                || !footerResolved.equals(st.footerRaw);
        final boolean teamNameChanged = !teamName.equals(st.teamName);
        final boolean listNameChanged = !listNameRaw.equals(st.listNameRaw);

        Team team = null;
        AppliedTeamState ts = null;
        boolean newlyCreated = false;

        boolean needsTeamWork = teamNameChanged;
        if (!needsTeamWork) {
            ts = teamStates.get(teamName);
            if (ts == null
                    || !tagPrefix.equals(ts.prefixRaw)
                    || !tagSuffix.equals(ts.suffixRaw)
                    || !ts.optionsApplied) {
                needsTeamWork = true;
            }
        }

        if (needsTeamWork) {
            team = scoreboard.getTeam(teamName);
            if (team == null) {
                try {
                    team = scoreboard.registerNewTeam(teamName);
                    ownedTeams.add(teamName);
                    newlyCreated = true;
                } catch (Throwable t) {
                    return;
                }
            }
            ts = teamStateOf(teamName);

            if (newlyCreated || !ts.optionsApplied) {
                try { team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER); } catch (Throwable ignored) {}
                ts.optionsApplied = true;
            }
            if (!tagPrefix.equals(ts.prefixRaw)) {
                ts.prefixRaw = tagPrefix;
                try { team.prefix(parseLegacy(tagPrefix)); } catch (Throwable ignored) {}
            }
            if (!tagSuffix.equals(ts.suffixRaw)) {
                ts.suffixRaw = tagSuffix;
                try { team.suffix(parseLegacy(tagSuffix)); } catch (Throwable ignored) {}
            }
        }

        if (teamNameChanged) {
            String oldTeamName = st.teamName;
            if (oldTeamName != null && !oldTeamName.equals(teamName)) {
                Team old = scoreboard.getTeam(oldTeamName);
                if (old != null && old.hasEntry(playerName)) {
                    try { old.removeEntry(playerName); } catch (Throwable ignored) {}
                    if (old.getEntries().isEmpty()) {
                        try { old.unregister(); } catch (Throwable ignored) {}
                        ownedTeams.remove(oldTeamName);
                        teamStates.remove(oldTeamName);
                    }
                }
            }
            if (team == null) team = scoreboard.getTeam(teamName);
            if (team != null && !team.hasEntry(playerName)) {
                try { team.addEntry(playerName); } catch (Throwable ignored) {}
            }
            st.teamName = teamName;
        } else {
            if (st.teamName == null || !teamName.equals(st.teamName)) {
                if (team == null) team = scoreboard.getTeam(teamName);
                if (team != null && !team.hasEntry(playerName)) {
                    try { team.addEntry(playerName); } catch (Throwable ignored) {}
                }
                st.teamName = teamName;
            }
        }

        st.prefixRaw = tagPrefix;
        st.suffixRaw = tagSuffix;

        if (listNameChanged) {
            st.listNameRaw = listNameRaw;
            try { p.playerListName(parseLegacy(listNameRaw)); } catch (Throwable ignored) {}
        }
        if (headerFooterChanged) {
            st.headerRaw = headerResolved;
            st.footerRaw = footerResolved;
            try {
                p.sendPlayerListHeaderAndFooter(
                        parseLegacyDirect(headerResolved),
                        parseLegacyDirect(footerResolved));
            } catch (Throwable ignored) {}
        }
    }

    private void applyForPlayer(Player p, String headerTpl, String footerTpl) {
        if (scoreboard == null) return;
        final UUID uuid = p.getUniqueId();
        final String playerName = p.getName();
        final TabPlayerData d = dataFor(p);
        final String group = d.primaryGroup();
        final int weight = d.weight();

        final String headerResolved = applyPlaceholders(p, headerTpl);
        final String footerResolved = applyPlaceholders(p, footerTpl);

        final GroupTpls tpl = groupTplsFor(group);
        final String tabPrefix = applyPlaceholders(p, tpl.tabPrefix);
        final String tabSuffix = applyPlaceholders(p, tpl.tabSuffix);
        final String tagPrefix = applyPlaceholders(p, tpl.tagPrefix);
        final String tagSuffix = applyPlaceholders(p, tpl.tagSuffix);
        final String listNameRaw = tabPrefix + playerName + tabSuffix;
        final String teamName = teamNameFor(p, group, weight);

        final AppliedState st = appliedStateOf(uuid);
        final boolean headerFooterChanged = !headerResolved.equals(st.headerRaw)
                || !footerResolved.equals(st.footerRaw);
        final boolean prefixChanged = !tagPrefix.equals(st.prefixRaw);
        final boolean suffixChanged = !tagSuffix.equals(st.suffixRaw);
        final boolean teamNameChanged = !teamName.equals(st.teamName);
        final boolean listNameChanged = !listNameRaw.equals(st.listNameRaw);

        if (!headerFooterChanged && !prefixChanged && !suffixChanged
                && !teamNameChanged && !listNameChanged) {
            return;
        }

        Team team = scoreboard.getTeam(teamName);
        boolean newlyCreated = false;
        if (team == null) {
            try {
                team = scoreboard.registerNewTeam(teamName);
                ownedTeams.add(teamName);
                newlyCreated = true;
            } catch (Throwable t) {
                return;
            }
        }
        if (newlyCreated || !st.teamOptionsApplied || !teamName.equals(st.teamName)) {
            try { team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER); } catch (Throwable ignored) {}
            st.teamOptionsApplied = true;
        }

        if (prefixChanged || newlyCreated) {
            st.prefixRaw = tagPrefix;
            try { team.prefix(LEGACY.deserialize(Text.color(tagPrefix))); } catch (Throwable ignored) {}
        }
        if (suffixChanged || newlyCreated) {
            st.suffixRaw = tagSuffix;
            try { team.suffix(LEGACY.deserialize(Text.color(tagSuffix))); } catch (Throwable ignored) {}
        }

        if (teamNameChanged) {
            String oldTeamName = st.teamName;
            if (oldTeamName != null && !oldTeamName.equals(teamName)) {
                Team old = scoreboard.getTeam(oldTeamName);
                if (old != null && old.hasEntry(playerName)) {
                    try { old.removeEntry(playerName); } catch (Throwable ignored) {}
                    if (old.getEntries().isEmpty()) {
                        try { old.unregister(); } catch (Throwable ignored) {}
                        ownedTeams.remove(oldTeamName);
                    }
                }
            }
            if (!team.hasEntry(playerName)) {
                try { team.addEntry(playerName); } catch (Throwable ignored) {}
            }
            st.teamName = teamName;
        } else if (!team.hasEntry(playerName)) {
            try { team.addEntry(playerName); } catch (Throwable ignored) {}
        }

        if (listNameChanged || newlyCreated) {
            st.listNameRaw = listNameRaw;
            try { p.playerListName(LEGACY.deserialize(Text.color(listNameRaw))); } catch (Throwable ignored) {}
        }
        if (headerFooterChanged) {
            st.headerRaw = headerResolved;
            st.footerRaw = footerResolved;
            try {
                p.sendPlayerListHeaderAndFooter(
                        LEGACY.deserialize(Text.color(headerResolved)),
                        LEGACY.deserialize(Text.color(footerResolved)));
            } catch (Throwable ignored) {}
        }
    }

    private void buildSnapshot(Collection<? extends Player> online) {
        int n = online.size();
        if (snapPlayers.length < n) {
            int newCap = Math.max(16, Integer.highestOneBit(n - 1) << 1);
            snapPlayers = new Player[newCap];
            snapGroups = new String[newCap];
            snapWeights = new int[newCap];
        }
        int i = 0;
        for (Player p : online) {
            TabPlayerData d = dataFor(p);
            snapPlayers[i] = p;
            snapGroups[i] = d.primaryGroup();
            snapWeights[i] = d.weight();
            i++;
        }
        for (int j = i; j < snapPlayers.length && snapPlayers[j] != null; j++) {
            snapPlayers[j] = null;
            snapGroups[j] = null;
        }
        snapSize = i;
    }

    private static String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    private void applyHeaderFooter(Player player) {
        applyHeaderFooter(player, tplHeader, tplFooter);
    }

    private void applyHeaderFooter(Player player, String headerTpl, String footerTpl) {
        String headerResolved = applyPlaceholders(player, headerTpl);
        String footerResolved = applyPlaceholders(player, footerTpl);
        AppliedState st = appliedStateOf(player.getUniqueId());
        if (headerResolved.equals(st.headerRaw) && footerResolved.equals(st.footerRaw)) return;
        st.headerRaw = headerResolved;
        st.footerRaw = footerResolved;
        try {
            player.sendPlayerListHeaderAndFooter(
                    LEGACY.deserialize(Text.color(headerResolved)),
                    LEGACY.deserialize(Text.color(footerResolved)));
        } catch (Throwable ignored) {
        }
    }

    private AppliedState appliedStateOf(UUID uuid) {
        AppliedState st = appliedCache.getIfPresent(uuid);
        if (st == null) {
            st = new AppliedState();
            appliedCache.put(uuid, st);
        }
        return st;
    }

    private AppliedTeamState teamStateOf(String teamName) {
        AppliedTeamState ts = teamStates.get(teamName);
        if (ts == null) {
            ts = new AppliedTeamState();
            teamStates.put(teamName, ts);
        }
        return ts;
    }

    private Component parseLegacy(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        Component cached = componentCache.get(raw);
        if (cached != null) return cached;
        Component c = LEGACY.deserialize(Text.color(raw));
        if (componentCache.size() >= COMPONENT_CACHE_CAP) {
            componentCache.clear();
        }
        componentCache.put(raw, c);
        return c;
    }

    private Component parseLegacyDirect(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        return LEGACY.deserialize(Text.color(raw));
    }

    private Component buildComponent(Player ctx, List<String> lines) {
        if (lines == null || lines.isEmpty()) return Component.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(applyPlaceholders(ctx, lines.get(i)));
        }
        return LEGACY.deserialize(Text.color(sb.toString()));
    }

    private void applyTeamFor(Player player) {
        TabPlayerData d = dataFor(player);
        applyTeamFor(player, d.primaryGroup(), d.weight());
    }

    private void applyTeamFor(Player player, String group, int weight) {
        if (scoreboard == null) return;
        String teamName = teamNameFor(player, group, weight);
        AppliedState st = appliedStateOf(player.getUniqueId());

        Team team = scoreboard.getTeam(teamName);
        boolean newlyCreated = false;
        if (team == null) {
            try {
                team = scoreboard.registerNewTeam(teamName);
                ownedTeams.add(teamName);
                newlyCreated = true;
            } catch (Throwable t) {
                return;
            }
        }
        if (newlyCreated) {
            st.prefixRaw = null;
            st.suffixRaw = null;
            st.listNameRaw = null;
            st.teamOptionsApplied = false;
        }
        if (newlyCreated || !st.teamOptionsApplied || !teamName.equals(st.teamName)) {
            try {
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            } catch (Throwable ignored) {
            }
            st.teamOptionsApplied = true;
        }

        GroupTpls tpl = groupTplsFor(group);
        String tabPrefix = applyPlaceholders(player, tpl.tabPrefix);
        String tabSuffix = applyPlaceholders(player, tpl.tabSuffix);
        String tagPrefix = applyPlaceholders(player, tpl.tagPrefix);
        String tagSuffix = applyPlaceholders(player, tpl.tagSuffix);

        if (!tagPrefix.equals(st.prefixRaw)) {
            st.prefixRaw = tagPrefix;
            try {
                team.prefix(LEGACY.deserialize(Text.color(tagPrefix)));
            } catch (Throwable ignored) {
            }
        }
        if (!tagSuffix.equals(st.suffixRaw)) {
            st.suffixRaw = tagSuffix;
            try {
                team.suffix(LEGACY.deserialize(Text.color(tagSuffix)));
            } catch (Throwable ignored) {
            }
        }

        if (!teamName.equals(st.teamName)) {
            if (st.teamName != null) {
                Team old = scoreboard.getTeam(st.teamName);
                if (old != null && old.hasEntry(player.getName())) {
                    try { old.removeEntry(player.getName()); } catch (Throwable ignored) {}
                    if (old.getEntries().isEmpty()) {
                        try { old.unregister(); } catch (Throwable ignored) {}
                        ownedTeams.remove(st.teamName);
                    }
                }
            }
            if (!team.hasEntry(player.getName())) {
                try { team.addEntry(player.getName()); } catch (Throwable ignored) {}
            }
            st.teamName = teamName;
        } else if (!team.hasEntry(player.getName())) {
            try { team.addEntry(player.getName()); } catch (Throwable ignored) {}
        }

        String listNameRaw = tabPrefix + player.getName() + tabSuffix;
        if (!listNameRaw.equals(st.listNameRaw)) {
            st.listNameRaw = listNameRaw;
            try {
                player.playerListName(LEGACY.deserialize(Text.color(listNameRaw)));
            } catch (Throwable ignored) {
            }
        }
    }

    private String teamNameFor(Player player, String group, int weight) {
        String key = sortKeyFor(group, weight);
        String g = (group == null || group.isEmpty()) ? "_" : group;
        String base = "z" + key + "_" + g;
        return base.length() > 16 ? base.substring(0, 16) : base;
    }

    private String sortKeyFor(String group, int weight) {
        switch (sortType) {
            case GROUPS:
            case GROUPS_THEN_NAME: {
                int idx = sortGroups.indexOf(group);
                if (idx < 0) idx = sortGroups.size();
                return String.format("%04d", Math.min(9999, idx));
            }
            case NAME:
                return "0000";
            case NONE:
                return "9999";
            case WEIGHT:
            case WEIGHT_THEN_NAME:
            default:
                return String.format("%04d", Math.max(0, Math.min(9999, weight)));
        }
    }

    private void loadSortConfig() {
        String raw = config.getString("sort-type", "WEIGHT_THEN_NAME");
        try {
            this.sortType = SortType.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            this.sortType = SortType.WEIGHT_THEN_NAME;
        }
        List<String> raws = config.getStringList("sort-groups");
        this.sortGroups = new ArrayList<>(raws.size());
        for (String g : raws) if (g != null) this.sortGroups.add(g.toLowerCase());
    }

    private String resolveGroupSection(String group) {
        if (group != null && !group.isEmpty() && config.isConfigurationSection("groups." + group)) {
            return "groups." + group;
        }
        return "groups._DEFAULT_";
    }

    private TabPlayerData dataFor(Player player) {
        TabPlayerData cached = playerCache.getIfPresent(player.getUniqueId());
        if (cached != null) return cached;
        String rawGroup = LuckPermsBridge.getPrimaryGroup(player);
        String group = rawGroup == null ? "" : rawGroup.toLowerCase();
        int weight = config.getInt("groups." + (rawGroup == null ? "_DEFAULT_" : rawGroup) + ".weight",
                config.getInt("groups._DEFAULT_.weight"));
        TabPlayerData data = new TabPlayerData(group, weight);
        playerCache.put(player.getUniqueId(), data);
        return data;
    }

    public void invalidate(UUID uuid) {
        playerCache.invalidate(uuid);
        appliedCache.invalidate(uuid);
        if (placeholderCache != null) placeholderCache.invalidate(uuid);
    }

    private String applyPlaceholders(Player ctx, String input) {
        if (input == null || input.isEmpty()) return "";
        if (input.indexOf('%') < 0) return input;
        PlaceholderCache cache = this.placeholderCache;
        if (cache == null) {
            return Placeholders.apply(ctx, input);
        }
        return cache.get(ctx.getUniqueId(), input, k -> Placeholders.apply(ctx, k));
    }

    private GroupTpls groupTplsFor(String group) {
        if (group == null || group.isEmpty()) return tplDefaultGroup;
        GroupTpls g = tplGroups.get(group);
        return (g != null) ? g : tplDefaultGroup;
    }

    @SuppressWarnings("unused")
    public List<Player> sortedPlayers() {
        buildSnapshot(Bukkit.getOnlinePlayers());
        final int n = snapSize;
        final Player[] pls = snapPlayers;
        final String[] grps = snapGroups;
        final int[] wts = snapWeights;

        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;

        Comparator<Integer> cmp;
        switch (sortType) {
            case GROUPS:
                cmp = Comparator.comparingInt(i -> groupIndex(grps[i]));
                break;
            case GROUPS_THEN_NAME:
                cmp = Comparator.<Integer>comparingInt(i -> groupIndex(grps[i]))
                        .thenComparing(i -> pls[i].getName(), String.CASE_INSENSITIVE_ORDER);
                break;
            case WEIGHT:
                cmp = Comparator.comparingInt(i -> wts[i]);
                break;
            case NAME:
                cmp = Comparator.comparing(i -> pls[i].getName(), String.CASE_INSENSITIVE_ORDER);
                break;
            case NONE: {
                List<Player> noneList = new ArrayList<>(n);
                noneList.addAll(Arrays.asList(pls).subList(0, n));
                return noneList;
            }
            case WEIGHT_THEN_NAME:
            default:
                cmp = Comparator.<Integer>comparingInt(i -> wts[i])
                        .thenComparing(i -> pls[i].getName(), String.CASE_INSENSITIVE_ORDER);
        }
        Arrays.sort(idx, cmp);

        List<Player> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(pls[idx[i]]);
        return out;
    }

    private int groupIndex(String group) {
        int idx = sortGroups.indexOf(group);
        return idx < 0 ? Integer.MAX_VALUE : idx;
    }
}
