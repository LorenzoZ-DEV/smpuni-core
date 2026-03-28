package dev.lorenzz.hirustuffs.commands;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.command.CommandSender;

public class HiruCommand {

    @Command(name = "hiru", description = "Hiru admin command")
    @Permission("hiru.admin")
    public void root(@Sender CommandSender sender) {
        Messages.sendRaw(sender, "&8&m---------&8[ &b&lHiru &8]&8&m---------");
        Messages.sendRaw(sender, "&b/hiru reload &7- Reload config");
        Messages.sendRaw(sender, "&b/hiru version &7- Show version");
        Messages.sendRaw(sender, "&8&m----------------------------");
    }

    @Subcommand(name = "reload", description = "Reload configuration")
    @Permission("hiru.admin")
    public void reload(@Sender CommandSender sender) {
        Hiru.getInstance().reloadAllConfigs();
        Messages.send(sender, "&aConfiguration reloaded successfully!");
    }

    @Subcommand(name = "version", description = "Show version")
    @Permission("hiru.admin")
    public void version(@Sender CommandSender sender) {
        Messages.send(sender, "&bHiru &7v" + Hiru.getInstance().getDescription().getVersion() + " &7by &blorenzz");
    }
}
