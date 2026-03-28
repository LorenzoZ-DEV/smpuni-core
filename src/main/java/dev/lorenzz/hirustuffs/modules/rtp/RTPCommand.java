package dev.lorenzz.hirustuffs.modules.rtp;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

import java.util.Set;

public class RTPCommand {

    private final RandomTeleportService rtpService;

    public RTPCommand(RandomTeleportService rtpService) {
        this.rtpService = rtpService;
    }

    @Command(name = "rtp", aliases = {"randomtp", "wild"}, description = "Random teleport", async = false)
    @Permission("hiru.rtp")
    public void rtp(@Sender Player player, @Optional("") @Named("world") String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            // No world specified -> open the GUI menu
            new RtpMenu(player).open();
        } else {
            // World specified directly -> teleport without menu
            rtpService.handleCommand(player, worldName);
        }
    }
}
