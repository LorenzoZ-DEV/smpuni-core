package dev.lorenzz.lxsmp.modules.adaptive;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class AdaptiveToolModule extends AbstractModule {

    private AdaptiveToolListener listener;

    public AdaptiveToolModule(LXSMP plugin) {
        super(plugin, "adaptive-tool");
    }

    @Override
    public void onEnable() {
        listener = new AdaptiveToolListener(plugin);
        registerListener(listener);
        plugin.getCommandManager().register(new GiveAdaptiveToolCommand());
        logger.info("[Solum] AdaptiveToolModule loaded.");
    }

    public AdaptiveToolListener getListener() {
        return listener;
    }
}


