package dev.lorenzz.solum.modules.broadcast;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("broadcast|bc")
@CommandPermission("solum.broadcast")
@Description("Broadcast a message to all players")
public class BroadcastCommand extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void broadcast(CommandSender sender, String[] args) {
        if (args == null || args.length == 0) {
            Messages.send(sender, "&#FF4444Usage: &f/broadcast <message>");
            return;
        }
        String message = String.join(" ", args).trim();
        if (message.isEmpty()) {
            Messages.send(sender, "&#FF4444Usage: &f/broadcast <message>");
            return;
        }

        String prefix = plugin.getConfigFile().getString("broadcast.prefix",
                "&#FF4444&l⚡ BROADCAST &#8C8C8C» ");
        String separator = plugin.getConfigFile().getString("broadcast.separator",
                "&8&m                                                    ");
        boolean showSeparator = plugin.getConfigFile().getBoolean("broadcast.show-separator", true);
        boolean playSound = plugin.getConfigFile().getBoolean("broadcast.play-sound", true);
        String soundName = plugin.getConfigFile().getString("broadcast.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (showSeparator) {
                Messages.sendRaw(online, "");
                Messages.sendRaw(online, separator);
            }
            Messages.sendRaw(online, prefix + message);
            if (showSeparator) {
                Messages.sendRaw(online, separator);
                Messages.sendRaw(online, "");
            }

            if (playSound) {
                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    online.playSound(online.getLocation(), sound, 1.0f, 1.0f);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        Bukkit.getConsoleSender().sendMessage(Messages.color(prefix + message));

        if (sender instanceof Player) {
            Messages.send(sender, "&#55FF55Broadcast sent!");
        }
    }
}

