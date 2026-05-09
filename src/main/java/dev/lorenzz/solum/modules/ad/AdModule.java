package dev.lorenzz.solum.modules.ad;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class AdModule extends AbstractModule {

    public AdModule(Solum plugin) {
        super(plugin, "ad");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new AdCommand());
        logger.info("[Solum] AdModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
