package dev.lorenzz.solum.modules.adaptive;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class AdaptiveToolModule extends AbstractModule {

    private AdaptiveToolListener listener;

    public AdaptiveToolModule(Solum plugin) {
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


