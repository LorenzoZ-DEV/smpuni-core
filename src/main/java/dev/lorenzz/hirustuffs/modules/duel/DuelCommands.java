package dev.lorenzz.hirustuffs.modules.duel;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.DuelManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

public class DuelCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "duel", description = "Challenge a player to a duel", async = false)
    @Permission("hiru.duel")
    public void duel(@Sender Player sender, @Named("player") Player target) {
        if (target.equals(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("duel.messages.self",
                    "&cYou can't duel yourself!"));
            return;
        }

        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(sender, "&cDuel system not available."); return; }

        if (dm.sendChallenge(sender, target)) {
            Messages.send(sender, plugin.getConfigFile().getString("duel.messages.challenge-sent",
                            "&7Duel challenge sent to &b%player%&7!")
                    .replace("%player%", target.getName()));
            Messages.send(target, plugin.getConfigFile().getString("duel.messages.challenge-received",
                            "&b%player% &7challenged you to a duel! Use &a/duelaccept &7or &c/dueldeny")
                    .replace("%player%", sender.getName()));
        }
    }

    @Command(name = "duelaccept", description = "Accept a duel challenge", async = false)
    @Permission("hiru.duel")
    public void duelAccept(@Sender Player sender) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(sender, "&cDuel system not available."); return; }

        if (dm.acceptChallenge(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("duel.messages.accepted",
                    "&a⚔ Duel accepted! Prepare to fight!"));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("duel.messages.no-challenge",
                    "&cYou have no pending duel challenges."));
        }
    }

    @Command(name = "dueldeny", description = "Deny a duel challenge", async = false)
    @Permission("hiru.duel")
    public void duelDeny(@Sender Player sender) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(sender, "&cDuel system not available."); return; }

        if (dm.denyChallenge(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("duel.messages.denied",
                    "&7Duel challenge denied."));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("duel.messages.no-challenge",
                    "&cYou have no pending duel challenges."));
        }
    }
}
