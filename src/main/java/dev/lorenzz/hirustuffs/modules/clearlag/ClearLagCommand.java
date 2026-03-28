package dev.lorenzz.hirustuffs.modules.clearlag;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.command.CommandSender;

public class ClearLagCommand {

    @Command(name = "clearlag", description = "Clear entities")
    @Permission("hiru.clearlag")
    public void clearlag(@Sender CommandSender sender) {
        Hiru plugin = Hiru.getInstance();
        int countdown = plugin.getConfigFile().getInt("clearlag.countdown-seconds", 30);
        // Access the module via ModuleManager
        ClearLagModule module = (ClearLagModule) plugin.getModuleManager().getModule("clearlag");
        if (module != null) {
            module.startCountdown(countdown);
        } else {
            Messages.send(sender, "&cClearLag not available.");
        }
    }
}
