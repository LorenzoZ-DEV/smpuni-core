package dev.lorenzz.solum.modules.profile;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.GUI;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("profile")
@CommandPermission("solum.profile")
@Description("View your profile or another player's profile")
public class ProfileCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void profile(Player sender, @Optional @Single String targetName) {
        OfflinePlayer target = (targetName == null || targetName.isEmpty())
                ? sender
                : Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            Messages.send(sender, "&#FF4444Player not found: &f" + targetName);
            return;
        }

        String title = plugin.getConfigFile().getString("profile.menu-title", "&#d1e0e0Profile of %player%")
                .replace("%player%", target.getName() == null ? targetName : target.getName());

        GUI.Builder builder = GUI.builder(title, 3).border(Material.BLACK_STAINED_GLASS_PANE);

        List<String> lore = new ArrayList<>();
        lore.add("&7Name: &b" + target.getName());
        lore.add("&7UUID: &8" + target.getUniqueId());
        lore.add("&7Online: " + (target.isOnline() ? "&aYes" : "&cNo"));
        lore.add("&7First Joined: &f" + (target.getFirstPlayed() > 0
                ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(target.getFirstPlayed()))
                : "Unknown"));
        lore.add("&7Last Seen: &f" + (target.getLastPlayed() > 0
                ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(target.getLastPlayed()))
                : "Unknown"));

        builder.item(13, Material.PLAYER_HEAD, "&b" + target.getName(), lore);
        builder.open(sender);
    }
}

