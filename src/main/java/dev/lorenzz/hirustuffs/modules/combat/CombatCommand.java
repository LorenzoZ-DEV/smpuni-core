package dev.lorenzz.hirustuffs.modules.combat;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

public class CombatCommand {

    @Command(name = "combattimer", aliases = {"ct", "combattag"}, description = "Check your combat tag status", async = false)
    public void combatTimer(@Sender Player player) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) {
            Messages.send(player, "&#FF4444Combat system is not available.");
            return;
        }

        if (!cm.isInCombat(player.getUniqueId())) {
            Messages.send(player, "&#55FF55✔ You are not in combat.");
            return;
        }

        long remaining = cm.getRemainingSeconds(player.getUniqueId());
        Messages.send(player, "&#FF4444⚔ You are in combat! &#FFD700" + remaining + "s &#8C8C8Cremaining.");
    }
}
