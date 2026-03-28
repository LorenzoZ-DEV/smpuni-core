package dev.lorenzz.hirustuffs.utils;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Messages {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /**
     * LRU cache for color-processed strings. Avoids re-running regex on repeated strings
     * (e.g. help menus, GUI items opened many times).
     */
    private static final int COLOR_CACHE_SIZE = 256;
    @SuppressWarnings("serial")
    private static final Map<String, String> COLOR_CACHE = new LinkedHashMap<>(COLOR_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > COLOR_CACHE_SIZE;
        }
    };

    private static YamlConfiguration config;
    private static String prefix = "&8[&bHiru&8] ";

    private Messages() {}

    public static void init(YamlConfiguration cfg) {
        config = cfg;
        prefix = cfg.getString("messages.prefix", "&8[&bHiru&8] ");
    }

    public static String getPrefix() {
        return prefix;
    }

    /**
     * Translates &amp; color codes and &#RRGGBB hex codes into section-sign formatted strings.
     * Results are cached to avoid repeated regex processing.
     */
    public static String color(String message) {
        if (message == null) return "";
        // Check cache first
        String cached;
        synchronized (COLOR_CACHE) {
            cached = COLOR_CACHE.get(message);
        }
        if (cached != null) return cached;

        // Process colors
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            StringBuilder hex = new StringBuilder("§x");
            for (char c : matcher.group(1).toCharArray()) {
                hex.append("§").append(c);
            }
            matcher.appendReplacement(buffer, hex.toString());
        }
        matcher.appendTail(buffer);
        String result = ChatColor.translateAlternateColorCodes('&', buffer.toString());

        // Store in cache
        synchronized (COLOR_CACHE) {
            COLOR_CACHE.put(message, result);
        }
        return result;
    }

    /**
     * Converts a legacy-colored string into an Adventure Component.
     */
    public static Component colorComponent(String message) {
        return LEGACY.deserialize(color(message));
    }

    public static void send(CommandSender sender, String message) {
        ((Audience) sender).sendMessage(LEGACY.deserialize(color(prefix + message)));
    }

    public static void sendRaw(CommandSender sender, String message) {
        ((Audience) sender).sendMessage(LEGACY.deserialize(color(message)));
    }

    public static void sendNoPrefix(CommandSender sender, String message) {
        ((Audience) sender).sendMessage(LEGACY.deserialize(color(message)));
    }

    public static String get(String path) {
        if (config == null) return path;
        return config.getString(path, path);
    }

    public static String get(String path, String def) {
        if (config == null) return def;
        return config.getString(path, def);
    }

    public static void sendConfigMessage(CommandSender sender, String configPath) {
        String msg = get(configPath);
        send(sender, msg);
    }

    public static void reload(YamlConfiguration cfg) {
        config = cfg;
        prefix = cfg.getString("messages.prefix", "&8[&bHiru&8] ");
    }

    /**
     * Formats a number with compact suffixes:
     * K (thousands), M (millions), B (billions), T (trillions), QT (quadrillions).
     * Numbers below 1000 are shown as-is.
     * Examples: 15000 → "15K", 1500000 → "1.5M", 2500000000 → "2.5B"
     */
    public static String formatNumber(double amount) {
        if (amount < 0) return "-" + formatNumber(-amount);
        if (amount < 1_000) {
            if (amount == (long) amount) return String.valueOf((long) amount);
            return String.format("%.2f", amount);
        }

        String[] suffixes = {"", "K", "M", "B", "T", "QT"};
        int index = 0;
        double value = amount;

        while (value >= 1_000 && index < suffixes.length - 1) {
            value /= 1_000;
            index++;
        }

        if (value == (long) value) {
            return String.format("%d%s", (long) value, suffixes[index]);
        }
        // Show 1 decimal if needed, strip trailing zero
        String formatted = String.format("%.1f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted + suffixes[index];
    }

    /**
     * Formats an integer with compact suffixes.
     */
    public static String formatNumber(int amount) {
        return formatNumber((double) amount);
    }

    /**
     * Formats an integer with compact suffixes.
     */
    public static String formatNumber(long amount) {
        return formatNumber((double) amount);
    }
}
