package dev.lorenzz.hirustuffs.modules.duel;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.DuelManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

public class DuelAdminCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "dueladmin", description = "Duel admin commands", async = false)
    @Permission("hiru.admin")
    public void duelAdmin(@Sender Player player) {
        Messages.send(player, "&e⚔ Duel Admin Commands:");
        Messages.sendNoPrefix(player, "&7/dueladmin setpos1 &8- &fSet arena position 1");
        Messages.sendNoPrefix(player, "&7/dueladmin setpos2 &8- &fSet arena position 2");
        Messages.sendNoPrefix(player, "&7/dueladmin setcenter &8- &fSet arena center");
        Messages.sendNoPrefix(player, "&7/dueladmin setdiespawn &8- &fSet duel end spawn");
    }

    @Subcommand(name = "setpos1", description = "Set arena position 1")
    public void setPos1(@Sender Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }

        dm.setPos1(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.pos1-set",
                "&aArena position 1 set!"));
    }

    @Subcommand(name = "setpos2", description = "Set arena position 2")
    public void setPos2(@Sender Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }

        dm.setPos2(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.pos2-set",
                "&aArena position 2 set!"));
    }

    @Subcommand(name = "setcenter", description = "Set arena center")
    public void setCenter(@Sender Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }

        dm.setCenter(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.center-set",
                "&aArena center set!"));
    }

    @Subcommand(name = "setdiespawn", description = "Set duel end/death spawn")
    public void setDieSpawn(@Sender Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }

        dm.setDieSpawn(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.diespawn-set",
                "&aDuel end/death spawn set!"));
    }
}
