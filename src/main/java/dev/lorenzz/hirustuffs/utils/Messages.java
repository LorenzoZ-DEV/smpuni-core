package dev.lorenzz.hirustuffs.utils;

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
     */
    public static String color(String message) {
        if (message == null) return "";
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
}
