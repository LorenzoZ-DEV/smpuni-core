package dev.lorenzz.solum.modules.combat;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.entity.Player;

@CommandAlias("combattimer|ct|combattag")
@Description("Check your combat tag status")
public class CombatCommand extends BaseCommand {

    @Default
    public void combatTimer(Player player) {
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

