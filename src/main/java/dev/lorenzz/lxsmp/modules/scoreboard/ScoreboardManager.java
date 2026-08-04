package dev.lorenzz.lxsmp.modules.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import dev.lorenzz.lxsmp.utils.PlaceholderCache;
import dev.lorenzz.lxsmp.utils.Placeholders;
import dev.lorenzz.lxsmp.utils.Text;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final Plugin plugin;
    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    private static final class BoardState {
        String lastTitle;
        List<String> lastLines;
    }
    private final Map<UUID, BoardState> state = new ConcurrentHashMap<>();

    private PlaceholderCache placeholderCache;

    private FileConfiguration config;
    private BukkitTask updateTask;

    private volatile String tplTitle = "";
    private volatile List<String> tplLines = Collections.emptyList();
    private volatile boolean enabled = true;

    public ScoreboardManager(Plugin plugin) {
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
        File file = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!file.exists()) {
            plugin.saveResource("scoreboard.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader def = new InputStreamReader(
                plugin.getResource("scoreboard.yml"), StandardCharsets.UTF_8)) {
            this.config.setDefaults(YamlConfiguration.loadConfiguration(def));
        } catch (Exception ignored) {
        }
        long ttlMs = Math.max(0L, config.getLong("scoreboard.placeholder-cache-ttl-ms"));
        this.placeholderCache = (ttlMs == 0L) ? null : new PlaceholderCache(ttlMs);

        this.enabled = config.getBoolean("scoreboard.enabled", true);
        String t = config.getString("scoreboard.title");
        this.tplTitle = (t == null) ? "" : t;
        List<String> lines = config.getStringList("scoreboard.lines");
        int cap = Math.min(lines.size(), 15);
        List<String> snap = new ArrayList<>(cap);
        for (int i = 0; i < cap; i++) snap.add(lines.get(i));
        this.tplLines = Collections.unmodifiableList(snap);
        this.updateInterval = Math.max(1L, config.getLong("scoreboard.update-interval"));
    }

    private long updateInterval = 20L;
    private int chunkCursor = 0;

    public void start() {
        if (!enabled) return;

        Player[] online = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (int i = 0; i < online.length; i++) {
            final Player ref = online[i];
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> { if (ref.isOnline()) createBoard(ref); }, 1L + (i / 4));
        }
        this.updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin, this::updateChunk, 1L, 1L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (FastBoard board : boards.values()) {
            if (!board.isDeleted()) board.delete();
        }
        boards.clear();
        state.clear();
        if (placeholderCache != null) placeholderCache.clear();
    }

    public void reload() {
        stop();
        reloadConfig();
        start();
    }

    public void createBoard(Player player) {
        if (!enabled) return;
        FastBoard existing = boards.get(player.getUniqueId());
        if (existing != null && !existing.isDeleted()) {
            update(player, existing);
            return;
        }
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        update(player, board);
    }

    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null && !board.isDeleted()) board.delete();
        state.remove(player.getUniqueId());
        if (placeholderCache != null) placeholderCache.invalidate(player.getUniqueId());
    }

    public void update(Player player) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) return;
        update(player, board);
    }

    public void updateAll() {
        final String titleTpl = tplTitle;
        final List<String> rawLines = tplLines;

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = boards.get(player.getUniqueId());
            if (board == null || board.isDeleted()) continue;
            try {
                pushUpdate(player, board, titleTpl, rawLines);
            } catch (Throwable t) {
                plugin.getLogger().warning("Scoreboard update failed for "
                        + player.getName() + ": " + t);
            }
        }
    }

    private void updateChunk() {
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

        final String titleTpl = tplTitle;
        final List<String> rawLines = tplLines;

        int end = Math.min(chunkCursor + perTick, arr.length);

        final int sliceLen = end - chunkCursor;
        final Player[] slicePlayers = new Player[sliceLen];
        final FastBoard[] sliceBoards = new FastBoard[sliceLen];
        int kept = 0;
        for (int i = chunkCursor; i < end; i++) {
            Player player = arr[i];
            if (player == null || !player.isOnline()) continue;
            FastBoard board = boards.get(player.getUniqueId());
            if (board == null || board.isDeleted()) continue;
            slicePlayers[kept] = player;
            sliceBoards[kept] = board;
            kept++;
        }
        chunkCursor = (end >= arr.length) ? 0 : end;
        if (kept == 0) return;

        final int sliceSize = kept;
        final int lineCount = rawLines.size();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String[] resolvedTitles = new String[sliceSize];
            final String[][] resolvedLines = new String[sliceSize][];
            for (int i = 0; i < sliceSize; i++) {
                Player p = slicePlayers[i];
                try {
                    resolvedTitles[i] = Text.color(applyPlaceholders(p, titleTpl));
                    String[] lines = new String[lineCount];
                    for (int li = 0; li < lineCount; li++) {
                        lines[li] = Text.color(applyPlaceholders(p, rawLines.get(li)));
                    }
                    resolvedLines[i] = lines;
                } catch (Throwable t) {
                    resolvedTitles[i] = null;
                    resolvedLines[i] = null;
                }
            }

            Bukkit.getScheduler().runTask(plugin,
                    () -> applyResolved(slicePlayers, sliceBoards, resolvedTitles, resolvedLines, sliceSize));
        });
    }

    private void applyResolved(Player[] players, FastBoard[] boardArr,
                               String[] titles, String[][] linesArr, int len) {
        for (int i = 0; i < len; i++) {
            Player player = players[i];
            FastBoard board = boardArr[i];
            String resolvedTitle = titles[i];
            String[] resolvedLineArr = linesArr[i];
            if (resolvedTitle == null || resolvedLineArr == null) continue;
            if (player == null || !player.isOnline()) continue;
            if (board == null || board.isDeleted()) continue;

            BoardState bs = state.computeIfAbsent(player.getUniqueId(), k -> new BoardState());

            if (!resolvedTitle.equals(bs.lastTitle)) {
                bs.lastTitle = resolvedTitle;
                try { board.updateTitle(resolvedTitle); } catch (Throwable ignored) {}
            }
            List<String> lines = Arrays.asList(resolvedLineArr);
            if (!lines.equals(bs.lastLines)) {
                bs.lastLines = lines;
                try { board.updateLines(lines); } catch (Throwable ignored) {}
            }
        }
    }

    private void update(Player player, FastBoard board) {
        pushUpdate(player, board, tplTitle, tplLines);
    }

    private void pushUpdate(Player player, FastBoard board,
                            String titleTpl, List<String> rawLines) {
        String resolvedTitle = Text.color(applyPlaceholders(player, titleTpl));

        int max = rawLines.size();
        List<String> lines = new ArrayList<>(max);
        for (int i = 0; i < max; i++) {
            lines.add(Text.color(applyPlaceholders(player, rawLines.get(i))));
        }

        BoardState bs = state.computeIfAbsent(player.getUniqueId(), k -> new BoardState());

        if (!resolvedTitle.equals(bs.lastTitle)) {
            bs.lastTitle = resolvedTitle;
            board.updateTitle(resolvedTitle);
        }
        if (!lines.equals(bs.lastLines)) {
            bs.lastLines = lines;
            board.updateLines(lines);
        }
    }

    private String applyPlaceholders(Player player, String input) {
        if (input == null || input.isEmpty()) return "";
        if (input.indexOf('%') < 0) return input;
        PlaceholderCache cache = this.placeholderCache;
        if (cache == null) {
            return Placeholders.apply(player, input);
        }
        return cache.get(player.getUniqueId(), input, k -> Placeholders.apply(player, k));
    }
}
