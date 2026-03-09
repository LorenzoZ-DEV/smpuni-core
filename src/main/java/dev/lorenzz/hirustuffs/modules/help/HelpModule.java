package dev.lorenzz.hirustuffs.modules.help;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class HelpModule extends AbstractModule {

    public HelpModule(Hiru plugin) {
        super(plugin, "help");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new HelpCommands());
        logger.info("[Hiru] HelpModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
