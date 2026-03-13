package dev.lorenzz.hirustuffs.modules.broadcast;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BroadcastCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "broadcast", aliases = {"bc"}, description = "Broadcast a message to all players", async = false)
    @Permission("hiru.broadcast")
    public void broadcast(@Sender CommandSender sender, @Combined @Named("message") String message) {
        if (message == null || message.trim().isEmpty()) {
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

        // Build the broadcast
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

        // Also send to console
        Bukkit.getConsoleSender().sendMessage(Messages.color(prefix + message));

        // Confirmation to sender if they are a player
        if (sender instanceof Player) {
            Messages.send(sender, "&#55FF55Broadcast sent!");
        }
    }
}
