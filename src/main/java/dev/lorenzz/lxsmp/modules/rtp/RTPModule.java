package dev.lorenzz.lxsmp.modules.rtp;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class RTPModule extends AbstractModule {

    private RandomTeleportService rtpService;

    public RTPModule(LXSMP plugin) {
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
