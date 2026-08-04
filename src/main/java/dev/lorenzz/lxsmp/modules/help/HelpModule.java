package dev.lorenzz.lxsmp.modules.help;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class HelpModule extends AbstractModule {

    public HelpModule(LXSMP plugin) {
        super(plugin, "help");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new HelpCommands());
        logger.info("[Solum] HelpModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
