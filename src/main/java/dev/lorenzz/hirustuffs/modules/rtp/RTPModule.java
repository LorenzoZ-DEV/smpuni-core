package dev.lorenzz.hirustuffs.modules.rtp;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class RTPModule extends AbstractModule {

    private RandomTeleportService rtpService;

    public RTPModule(Hiru plugin) {
        super(plugin, "rtp");
    }

    @Override
    public void onEnable() {
        rtpService = new RandomTeleportService(plugin);
        plugin.getCommandManager().register(new RTPCommand(rtpService));
        logger.info("[Hiru] RTPModule loaded.");
    }

    @Override
    public void onReload() {
        if (rtpService != null) {
            rtpService.reload();
        }
        logger.info("[Hiru] RTPModule reloaded.");
    }

    public RandomTeleportService getRtpService() {
        return rtpService;
    }
}
