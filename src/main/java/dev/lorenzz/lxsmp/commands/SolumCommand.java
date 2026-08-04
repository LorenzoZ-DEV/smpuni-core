package dev.lorenzz.lxsmp.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.command.CommandSender;

@CommandAlias("luxurysmp|lxsmp")
@CommandPermission("solum.admin")
@Description("Solum admin command")
public class SolumCommand extends BaseCommand {

    @Default
    @HelpCommand
    @CatchUnknown
    public void root(CommandSender sender) {
        Messages.sendRaw(sender, "&8&m---------&8[ &5&lLuxurySMP &8]&8&m---------");
        Messages.sendRaw(sender, "&b/lxsmp reload &7- Reload config");
        Messages.sendRaw(sender, "&b/lxsmp version &7- Show version");
        Messages.sendRaw(sender, "&8&m----------------------------");
    }

    @Subcommand("reload")
    @Description("Reload configuration")
    public void reload(CommandSender sender) {
        LXSMP.getInstance().reloadAllConfigs();
        Messages.send(sender, "&aConfiguration reloaded successfully!");
    }

    @Subcommand("version")
    @Description("Show version")
    public void version(CommandSender sender) {
        Messages.send(sender, "&5LuxurySMP &7v" + LXSMP.getInstance().getDescription().getVersion() + " &7by &5Lorenzzzz");
    }
}
