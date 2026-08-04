package dev.lorenzz.lxsmp.modules.afk;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.lxsmp.managers.AFKManager;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.entity.Player;

@CommandAlias("afk")
@CommandPermission("solum.afk")
@Description("Toggle AFK")
public class AFKCommand extends BaseCommand {

    @Default
    public void afk(Player player) {
        AFKManager afk = AFKManager.getInstance();
        if (afk == null) { Messages.send(player, "&cAFK not available."); return; }

        boolean goingAfk = !afk.isAFK(player);
        if (goingAfk) {
            if (!afk.teleportToAFKZone(player)) {
                Messages.send(player, "&cNo AFK Zone set! You have been marked as AFK without teleporting.");
            }
            afk.setAFK(player, true);
        } else {
            afk.setAFK(player, false);
        }
    }
}

@CommandAlias("setafkzone")
@CommandPermission("solum.admin")
@Description("Set an AFK Zone around your position")
class SetAfkZoneCommand extends BaseCommand {
    @Default
    public void setAfkZone(Player player, @Optional String name, @Optional Integer radius) {
        if (name == null || name.isEmpty()) name = "afkzone";
        int r = (radius == null) ? 15 : radius;
        AFKManager afk = AFKManager.getInstance();
        if (afk == null) { Messages.send(player, "&cAFK not available."); return; }
        if (r < 1 || r > 500) {
            Messages.send(player, "&cRadius must be between 1 and 500!");
            return;
        }
        afk.setAFKZone(name, player.getLocation(), r);
        Messages.send(player, "&7AFK Zone &b" + name + " &7set with radius &b" + r + " &7at your position!");
    }
}

@CommandAlias("removeafkzone")
@CommandPermission("solum.admin")
@Description("Remove an AFK Zone")
class RemoveAfkZoneCommand extends BaseCommand {
    @Default
    public void removeAfkZone(Player player, String name) {
        AFKManager afk = AFKManager.getInstance();
        if (afk == null) { Messages.send(player, "&cAFK not available."); return; }
        if (afk.removeAFKZone(name)) {
            Messages.send(player, "&7AFK Zone &b" + name + " &7removed!");
        } else {
            Messages.send(player, "&cAFK Zone &f" + name + " &cnot found!");
        }
    }
}

@CommandAlias("afkzones")
@CommandPermission("solum.admin")
@Description("List all AFK Zones")
class AfkZonesListCommand extends BaseCommand {
    @Default
    public void listAfkZones(Player player) {
        AFKManager afk = AFKManager.getInstance();
        if (afk == null) { Messages.send(player, "&cAFK not available."); return; }
        var zones = afk.getAFKZoneNames();
        if (zones.isEmpty()) {
            Messages.send(player, "&7No AFK Zones set.");
            return;
        }
        Messages.send(player, "&7AFK Zones: &b" + String.join(", ", zones));
    }
}
