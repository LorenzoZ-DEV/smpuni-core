package dev.lorenzz.solum.modules.info;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class InfoModule extends AbstractModule {

    public InfoModule(Solum plugin) {
        super(plugin, "info");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new InfoCommands());
        logger.info("[Solum] InfoModule loaded.");
    }
}
