package dev.lorenzz.lxsmp.modules.clearlag;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.command.CommandSender;

@CommandAlias("clearlag")
@CommandPermission("solum.clearlag")
@Description("Clear entities")
public class ClearLagCommand extends BaseCommand {

    @Default
    public void clearlag(CommandSender sender) {
        LXSMP plugin = LXSMP.getInstance();
        int countdown = plugin.getConfigFile().getInt("clearlag.countdown-seconds", 30);
        ClearLagModule module = plugin.getModuleManager().getModule("clearlag");
        if (module != null) {
            module.startCountdown(countdown);
        } else {
            Messages.send(sender, "&cClearLag not available.");
        }
    }
}

