package dev.lorenzz.lxsmp.modules.profile;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.GUI;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@CommandAlias("profile")
@CommandPermission("solum.profile")
public class ProfileCommands extends BaseCommand {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void profile(Player sender, @Optional @Single String targetName) {
        OfflinePlayer target = resolveTarget(sender, targetName);

        if (target == null) {
            Messages.send(sender, "&#FF4444Player not found: &f" + targetName);
            return;
        }

        String resolvedName = target.getName() != null ? target.getName() : targetName;

        String title = plugin.getConfigFile()
                .getString("profile.menu-title")
                .replace("%player%", resolvedName);

        GUI.Builder builder = GUI.builder(title, 3).border(Material.BLACK_STAINED_GLASS_PANE);

        List<String> lore = new ArrayList<>();
        lore.add("&7Name: &b" + resolvedName);
        lore.add("&7UUID: &8" + target.getUniqueId());
        lore.add("&7Online: " + (target.isOnline() ? "&aYes" : "&cNo"));
        lore.add("&7First Joined: &f" + formatDate(target.getFirstPlayed(), DATE_FORMAT));
        lore.add("&7Last Seen: &f" + formatDate(target.getLastPlayed(), DATETIME_FORMAT));

        builder.item(13, Material.PLAYER_HEAD, "&b" + resolvedName, lore);
        builder.open(sender);
    }

    private OfflinePlayer resolveTarget(Player sender, String targetName) {
        if (targetName == null || targetName.isEmpty()) {
            return sender;
        }

        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            return online;
        }

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(targetName);
        if (cached != null && (cached.hasPlayedBefore() || cached.isOnline())) {
            return cached;
        }

        return null;
    }

    private String formatDate(long timestamp, DateTimeFormatter formatter) {
        if (timestamp <= 0) {
            return "Unknown";
        }
        return formatter.format(Instant.ofEpochMilli(timestamp));
    }
}

