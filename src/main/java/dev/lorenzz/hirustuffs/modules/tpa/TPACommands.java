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

    @Command(name = "tpa", description = "Richiedi teletrasporto", async = false)
    @Permission("hiru.tpa")
    public void tpa(@Sender Player sender, @Named("giocatore") Player target) {
        if (target.equals(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.self", "&cNon puoi inviare una TPA a te stesso!"));
            return;
        }
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA non disponibile."); return; }

        if (tpa.sendRequest(sender, target)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.sent", "&7Richiesta TPA inviata a %player%.")
                    .replace("%player%", target.getName()));
            Messages.send(target, plugin.getConfigFile().getString("tpa.messages.received", "&7%player% vuole teletrasportarsi da te! &a/tpaccept &7o &c/tpdeny")
                    .replace("%player%", sender.getName()));
        }
    }

    @Command(name = "tpaccept", description = "Accetta richiesta TPA", async = false)
    public void tpaccept(@Sender Player sender) {
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA non disponibile."); return; }

        if (tpa.acceptRequest(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.accepted", "&7Richiesta TPA accettata!"));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.no-request", "&cNessuna richiesta TPA pendente."));
        }
    }

    @Command(name = "tpdeny", description = "Rifiuta richiesta TPA", async = false)
    public void tpdeny(@Sender Player sender) {
        TPAManager tpa = TPAManager.getInstance();
        if (tpa == null) { Messages.send(sender, "&cTPA non disponibile."); return; }

        if (tpa.denyRequest(sender)) {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.denied", "&7Richiesta TPA rifiutata."));
        } else {
            Messages.send(sender, plugin.getConfigFile().getString("tpa.messages.no-request", "&cNessuna richiesta TPA pendente."));
        }
    }
}
