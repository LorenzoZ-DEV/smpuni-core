package dev.lorenzz.hirustuffs.modules.tpa;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Named;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.TPAManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

public class TPACommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "tpa", description = "Request teleport", async = false)
    @Permission("hiru.tpa")
    public void tpa(@Sender Player sender, @Named("player") Player target) {
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

   @Command(name = "tpahere", description = "Request a player to teleport to you", async = false)
    @Permission("hiru.tpahere")
    public void tpahere(@Sender Player sender, @Named("player") Player target) {
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

    @Command(name = "tpaccept", description = "Accept TPA request", async = false)
    public void tpaccept(@Sender Player sender) {
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA not available."); return; }

        if (tpa.acceptRequest(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.accepted", "&7TPA request accepted!"));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.no-request", "&cNo pending TPA requests."));
        }
    }

    @Command(name = "tpdeny", description = "Deny TPA request", async = false)
    public void tpdeny(@Sender Player sender) {
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA not available."); return; }

        if (tpa.denyRequest(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.denied", "&7TPA request denied."));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.no-request", "&cNo pending TPA requests."));
        }
    }
}
