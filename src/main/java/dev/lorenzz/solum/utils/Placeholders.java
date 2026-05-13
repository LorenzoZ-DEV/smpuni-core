package dev.lorenzz.solum.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class Placeholders {

    private static final int MAX_PASSES = 2;

    private static volatile long lastCheckNanos = 0L;
    private static volatile boolean cachedAvailable = false;
    private static final long CHECK_INTERVAL_NS = 5_000_000_000L;

    private Placeholders() {}

    public static boolean isAvailable() {
        long now = System.nanoTime();
        if (now - lastCheckNanos > CHECK_INTERVAL_NS) {
            cachedAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
            lastCheckNanos = now;
        }
        return cachedAvailable;
    }

    public static void invalidateAvailability() {
        lastCheckNanos = 0L;
    }

    public static String apply(Player player, String input) {
        if (input == null || input.isEmpty()) return input;
        if (input.indexOf('%') < 0) return input;
        String s = BuiltinPlaceholders.apply(player, input);
        if (player == null || s.indexOf('%') < 0 || !isAvailable()) return s;
        try {
            String previous;
            String current = s;
            int passes = 0;
            do {
                previous = current;
                current = PlaceholderAPI.setPlaceholders(player, previous);
                if (current.indexOf('%') < 0) return current;
                current = BuiltinPlaceholders.apply(player, current);
                passes++;
            } while (!current.equals(previous) && passes < MAX_PASSES && current.indexOf('%') >= 0);
            return current;
        } catch (Throwable t) {
            return s;
        }
    }

    public static String apply(OfflinePlayer player, String input) {
        if (input == null || input.isEmpty()) return input;
        Player online = (player != null) ? player.getPlayer() : null;
        String s = BuiltinPlaceholders.apply(online, input);
        if (player == null || !isAvailable()) return s;
        try {
            String previous;
            String current = s;
            int passes = 0;
            do {
                previous = current;
                current = PlaceholderAPI.setPlaceholders(player, previous);
                if (online != null) current = BuiltinPlaceholders.apply(online, current);
                passes++;
            } while (!current.equals(previous) && passes < MAX_PASSES && current.indexOf('%') >= 0);
            return current;
        } catch (Throwable t) {
            return s;
        }
    }
}
