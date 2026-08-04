package dev.lorenzz.lxsmp.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

final class BuiltinPlaceholders {

    private BuiltinPlaceholders() {}

    static String apply(Player player, String input) {
        if (input == null || input.isEmpty()) return input;

        if (input.indexOf('%') < 0) return input;
        String s = input;
        if (player != null) {
            s = s.replace("%player%", player.getName())
                    .replace("%player_name%", player.getName())
                    .replace("%world%", player.getWorld().getName())
                    .replace("%ping%", String.valueOf(player.getPing()));
        }
        if (s.indexOf('%') < 0) return s;
        s = s.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        return s;
    }
}

