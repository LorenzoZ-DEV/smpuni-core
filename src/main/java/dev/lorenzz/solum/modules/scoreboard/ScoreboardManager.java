package dev.lorenzz.solum.modules.scoreboard;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Config;
import dev.lorenzz.solum.utils.Messages;
import fr.mrmicky.fastboard.FastBoard;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages per-player FastBoard scoreboards. Loads layout from scoreboard.yml.
 */
public class ScoreboardManager {


    private final Solum plugin;
    private final Config config;
    private final Map<UUID, FastBoard> boards = new HashMap<>();
    private BukkitTask updateTask;

    public ScoreboardManager(Solum plugin) {
        this.plugin = plugin;
        this.config = new Config(plugin, "scoreboard.yml");
    }

    public Config getConfig() {
        return config;
    }

    public void start() {
        if (!config.getBoolean("scoreboard.enabled", true)) {
            plugin.getLogger().info("[Solum] Scoreboard disabled in scoreboard.yml.");
            return;
        }
        // Create boards for already-online players (e.g. /reload)
        for (Player player : Bukkit.getOnlinePlayers()) {
            createBoard(player);
        }
        long interval = Math.max(1L, config.getLong("scoreboard.update-interval", 20L));
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
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
    }

    public void createBoard(Player player) {
        if (!config.getBoolean("scoreboard.enabled", true)) return;
        FastBoard board = boards.get(player.getUniqueId());
        if (board != null && !board.isDeleted()) return;
        board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        update(player, board);
    }

    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null && !board.isDeleted()) board.delete();
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = boards.get(player.getUniqueId());
            if (board == null || board.isDeleted()) continue;
            update(player, board);
        }
    }

    private void update(Player player, FastBoard board) {
        String title = config.getString("scoreboard.title", "&bSolum");
        List<String> rawLines = config.getStringList("scoreboard.lines");

        board.updateTitle(Messages.color(applyPlaceholders(player, title)));

        List<String> lines = new ArrayList<>(rawLines.size());
        for (String line : rawLines) {
            lines.add(Messages.color(applyPlaceholders(player, line)));
        }
        // FastBoard supports up to 15 lines
        if (lines.size() > 15) lines = lines.subList(0, 15);
        board.updateLines(lines);
    }

    private String applyPlaceholders(Player player, String input) {
        if (input == null) return "";
        String s = input.replace("{name}", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                s = PlaceholderAPI.setPlaceholders(player, s);
            } catch (Throwable ignored) {
                // Defensive: never fail the tick on placeholder errors
            }
        }
        return s;
    }
}

