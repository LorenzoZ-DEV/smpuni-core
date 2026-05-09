package dev.lorenzz.solum.modules.rtp;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import org.bukkit.entity.Player;

@CommandAlias("rtp|randomtp|wild")
@CommandPermission("solum.rtp")
@Description("Randomly teleport to a far-away location")
public class RTPCommand extends BaseCommand {

    private final RandomTeleportService service;

    public RTPCommand(RandomTeleportService service) {
        this.service = service;
    }

    @Default
    @CommandCompletion("@nothing|world|world_nether|world_the_end")
    public void rtp(Player player, @Optional @Single String world) {
        if (world == null || world.isEmpty()) {
            new RtpMenu(player).open();
            return;
        }
        service.handleCommand(player, world);
    }
}

