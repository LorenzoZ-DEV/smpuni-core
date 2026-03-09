package dev.lorenzz.hirustuffs.modules.afk;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.managers.AFKManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

public class AFKCommand {

    @Command(name = "afk", description = "Toggle AFK")
    @Permission("hiru.afk")
    public void afk(@Sender Player player) {
        AFKManager afk = AFKManager.getInstance();
        if (afk == null) { Messages.send(player, "&cAFK non disponibile."); return; }
        afk.toggleAFK(player);
    }
}
