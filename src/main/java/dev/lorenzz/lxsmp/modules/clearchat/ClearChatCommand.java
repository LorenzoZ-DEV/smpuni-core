package dev.lorenzz.lxsmp.modules.clearchat;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("clearchat|cc")
@CommandPermission("solum.clearchat")
@Description("Clear the chat for all players")
public class ClearChatCommand extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void clearChat(CommandSender sender, @Optional String flag) {
        boolean silent = "-silent".equalsIgnoreCase(flag);

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
                    online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1.2f);
                } catch (Exception ignored) {}
            }
        }

        Bukkit.getConsoleSender().sendMessage(Messages.color(
                "&8[&bSolum&8] &fChat cleared by " + clearedBy));

        if (sender instanceof Player) {
            Messages.send(sender, "&#55FF55Chat cleared successfully!");
        }
    }
}

