package dev.lorenzz.hirustuffs.modules.info;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class InfoModule extends AbstractModule {

    public InfoModule(Hiru plugin) {
        super(plugin, "info");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new InfoCommands());
        logger.info("[Hiru] InfoModule loaded.");
    }
}
