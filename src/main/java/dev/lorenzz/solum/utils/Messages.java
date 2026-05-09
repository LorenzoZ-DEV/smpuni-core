package dev.lorenzz.solum.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Messages {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final Cache<String, String> COLOR_CACHE = Caffeine.newBuilder()
            .maximumSize(512)
            .build();

    private static YamlConfiguration config;
    private static String prefix = "&8[&bSolum&8] ";

    private Messages() {}

    public static void init(YamlConfiguration cfg) {
        config = cfg;
        prefix = cfg.getString("messages.prefix", "&8[&bSolum&8] ");
    }

    public static String getPrefix() {
        return prefix;
    }

    public static String color(String message) {
        if (message == null) return "";
        return COLOR_CACHE.get(message, Messages::translateColors);
    }

    private static String translateColors(String message) {
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
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

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
        prefix = cfg.getString("messages.prefix", "&8[&bSolum&8] ");
    }

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
        String formatted = String.format("%.1f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted + suffixes[index];
    }

    public static String formatNumber(int amount) {
        return formatNumber((double) amount);
    }

    public static String formatNumber(long amount) {
        return formatNumber((double) amount);
    }
}
