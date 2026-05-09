package dev.lorenzz.solum.modules.tpa;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.TPAManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.entity.Player;

public class TPACommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @CommandAlias("tpa")
    @CommandPermission("solum.tpa")
    @Description("Request teleport")
    public void tpa(Player sender, Player target) {
        if (target.equals(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.self", "&cYou can't send a TPA to yourself!"));
            return;
        }
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA not available."); return; }

        if (tpa.sendRequest(sender, target)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.sent", "&7TPA request sent to &b%player%&7.")
                    .replace("%player%", target.getName()));
            Messages.send(target, plugin.getConfigFile().getString("tpa.messages.received", "&7&b%player% &7wants to teleport to you! &a/tpaccept &7or &c/tpdeny")
                    .replace("%player%", sender.getName()));
        }
    }

    @CommandAlias("tpahere")
    @CommandPermission("solum.tpahere")
    @Description("Request a player to teleport to you")
    public void tpahere(Player sender, Player target) {
        if (target.equals(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.self", "&cYou can't send a TPA to yourself!"));
            return;
        }
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA not available."); return; }

        if (tpa.sendHereRequest(sender, target)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.tpahere-sent", "&7TPAHere request sent to &b%player%&7.")
                    .replace("%player%", target.getName()));
            Messages.send(target, plugin.getConfigFile().getString("tpa.messages.tpahere-received",
                            "&7&b%player% &7wants you to teleport to them! &a/tpaccept &7or &c/tpdeny")
                    .replace("%player%", sender.getName()));
        }
    }

    @CommandAlias("tpaccept")
    @Description("Accept TPA request")
    public void tpaccept(Player sender) {
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA not available."); return; }

        if (tpa.acceptRequest(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.accepted", "&7TPA request accepted!"));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.no-request", "&cNo pending TPA requests."));
        }
    }

    @CommandAlias("tpdeny")
    @Description("Deny TPA request")
    public void tpdeny(Player sender) {
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA not available."); return; }

        if (tpa.denyRequest(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.denied", "&7TPA request denied."));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.no-request", "&cNo pending TPA requests."));
        }
    }
}
