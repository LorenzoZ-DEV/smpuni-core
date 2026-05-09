package dev.lorenzz.solum.modules.help;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class HelpModule extends AbstractModule {

    public HelpModule(Solum plugin) {
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
