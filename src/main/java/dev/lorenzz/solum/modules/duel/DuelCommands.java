package dev.lorenzz.solum.modules.duel;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.DuelManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.entity.Player;

@CommandAlias("duel")
@CommandPermission("solum.duel")
@Description("Challenge a player to a duel")
public class DuelCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void duel(Player sender, Player target) {
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
}

@CommandAlias("duelaccept")
@CommandPermission("solum.duel")
@Description("Accept a duel challenge")
class DuelAcceptCommand extends BaseCommand {
    private final Solum plugin = Solum.getInstance();

    @Default
    public void duelAccept(Player sender) {
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
}

@CommandAlias("dueldeny")
@CommandPermission("solum.duel")
@Description("Deny a duel challenge")
class DuelDenyCommand extends BaseCommand {
    private final Solum plugin = Solum.getInstance();

    @Default
    public void duelDeny(Player sender) {
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

