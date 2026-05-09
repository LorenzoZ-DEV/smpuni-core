package dev.lorenzz.solum.modules.rtp;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class RTPModule extends AbstractModule {

    private RandomTeleportService rtpService;

    public RTPModule(Solum plugin) {
        super(plugin, "rtp");
    }

    @Override
    public void onEnable() {
        rtpService = new RandomTeleportService(plugin);
        plugin.getCommandManager().register(new RTPCommand(rtpService));
        registerListener(new RtpPortalListener(plugin, rtpService));
        logger.info("[Solum] RTPModule loaded.");
    }

    @Override
    public void onReload() {
        if (rtpService != null) {
            rtpService.reload();
        }
        logger.info("[Solum] RTPModule reloaded.");
    }

    public RandomTeleportService getRtpService() {
        return rtpService;
    }
}
