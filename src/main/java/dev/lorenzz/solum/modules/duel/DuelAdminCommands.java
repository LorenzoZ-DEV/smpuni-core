package dev.lorenzz.solum.modules.duel;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.DuelManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.entity.Player;

@CommandAlias("dueladmin")
@CommandPermission("solum.admin")
@Description("Duel admin commands")
public class DuelAdminCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void duelAdmin(Player player) {
        Messages.send(player, "&e⚔ Duel Admin Commands:");
        Messages.sendNoPrefix(player, "&7/dueladmin setpos1 &8- &fSet arena position 1");
        Messages.sendNoPrefix(player, "&7/dueladmin setpos2 &8- &fSet arena position 2");
        Messages.sendNoPrefix(player, "&7/dueladmin setcenter &8- &fSet arena center");
        Messages.sendNoPrefix(player, "&7/dueladmin setdiespawn &8- &fSet duel end spawn");
    }

    @Subcommand("setpos1")
    @Description("Set arena position 1")
    public void setPos1(Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }
        dm.setPos1(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.pos1-set",
                "&aArena position 1 set!"));
    }

    @Subcommand("setpos2")
    @Description("Set arena position 2")
    public void setPos2(Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }
        dm.setPos2(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.pos2-set",
                "&aArena position 2 set!"));
    }

    @Subcommand("setcenter")
    @Description("Set arena center")
    public void setCenter(Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }
        dm.setCenter(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.center-set",
                "&aArena center set!"));
    }

    @Subcommand("setdiespawn")
    @Description("Set duel end/death spawn")
    public void setDieSpawn(Player player) {
        DuelManager dm = DuelManager.getInstance();
        if (dm == null) { Messages.send(player, "&cDuel system not available."); return; }
        dm.setDieSpawn(player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("duel.messages.diespawn-set",
                "&aDuel end/death spawn set!"));
    }
}

