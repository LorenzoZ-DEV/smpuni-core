package dev.lorenzz.hirustuffs.modules.afk;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Named;
import dev.lorenzz.common.annotations.Optional;
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
        if (afk == null) { Messages.send(player, "&cAFK not available."); return; }
        afk.toggleAFK(player);
    }

    @Command(name = "setafkzone", description = "Set an AFK Zone around your position")
    @Permission("hiru.admin")
    public void setAfkZone(@Sender Player player, @Optional("afkzone") @Named("name") String name, @Optional("15") @Named("radius") int radius) {
        AFKManager afk = AFKManager.getInstance();
        if (afk == null) { Messages.send(player, "&cAFK not available."); return; }
        if (radius < 1 || radius > 500) {
            Messages.send(player, "&cRadius must be between 1 and 500!");
            return;
        }
        afk.setAFKZone(name, player.getLocation(), radius);
        Messages.send(player, "&7AFK Zone &b" + name + " &7set with radius &b" + radius + " &7at your position!");
    }

    @Command(name = "removeafkzone", description = "Remove an AFK Zone")
    @Permission("hiru.admin")
    public void removeAfkZone(@Sender Player player, @Named("name") String name) {
        AFKManager afk = AFKManager.getInstance();
        if (afk == null) { Messages.send(player, "&cAFK not available."); return; }
        if (afk.removeAFKZone(name)) {
            Messages.send(player, "&7AFK Zone &b" + name + " &7removed!");
        } else {
            Messages.send(player, "&cAFK Zone &f" + name + " &cnot found!");
        }
    }

    @Command(name = "afkzones", description = "List all AFK Zones")
    @Permission("hiru.admin")
    public void listAfkZones(@Sender Player player) {
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
