package dev.lorenzz.lxsmp.modules.info;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class InfoModule extends AbstractModule {

    public InfoModule(LXSMP plugin) {
        super(plugin, "info");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new InfoCommands());
        logger.info("[Solum] InfoModule loaded.");
    }
}
