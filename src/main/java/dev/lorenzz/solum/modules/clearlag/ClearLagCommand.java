package dev.lorenzz.solum.modules.clearlag;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.command.CommandSender;

@CommandAlias("clearlag")
@CommandPermission("solum.clearlag")
@Description("Clear entities")
public class ClearLagCommand extends BaseCommand {

    @Default
    public void clearlag(CommandSender sender) {
        Solum plugin = Solum.getInstance();
        int countdown = plugin.getConfigFile().getInt("clearlag.countdown-seconds", 30);
        ClearLagModule module = plugin.getModuleManager().getModule("clearlag");
        if (module != null) {
            module.startCountdown(countdown);
        } else {
            Messages.send(sender, "&cClearLag not available.");
        }
    }
}

