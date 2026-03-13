package dev.lorenzz.hirustuffs.modules.clearchat;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChatCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "clearchat", aliases = {"cc"}, description = "Clear the chat for all players", async = false)
    @Permission("hiru.clearchat")
    public void clearChat(@Sender CommandSender sender, @Flag("-silent") boolean silent) {
        // Send 100 blank lines to every online player
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                online.sendMessage("");
            }
        }

        String clearedBy = (sender instanceof Player) ? sender.getName() : "Console";

        if (!silent) {
            String separator = "&8&m                                                    ";
            String msg = plugin.getConfigFile().getString("clearchat.message",
                    "&#FF4444&l⚡ &#8C8C8CChat has been cleared by &#55FF55%player%");
            msg = msg.replace("%player%", clearedBy);

            for (Player online : Bukkit.getOnlinePlayers()) {
                Messages.sendRaw(online, separator);
                Messages.sendRaw(online, msg);
                Messages.sendRaw(online, separator);

                try {
                    online.playSound(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                } catch (Exception ignored) {}
            }
        }

        // Log to console
        Bukkit.getConsoleSender().sendMessage(Messages.color(
                "&8[&bHiru&8] &fChat cleared by " + clearedBy));

        if (sender instanceof Player) {
            Messages.send(sender, "&#55FF55Chat cleared successfully!");
        }
    }
}
