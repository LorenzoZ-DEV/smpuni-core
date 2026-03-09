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

    @Command(name = "rtp", aliases = {"randomtp", "wild"}, description = "Teletrasporto casuale", async = false)
    @Permission("hiru.rtp")
    public void rtp(@Sender Player player, @Optional("") @Named("world") String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            // Nessun mondo specificato -> apri il menu GUI
            new RtpMenu(player).open();
        } else {
            // Mondo specificato direttamente -> teletrasporta senza menu
            rtpService.handleCommand(player, worldName);
        }
    }
}
