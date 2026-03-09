package dev.lorenzz.hirustuffs.modules.keyall;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class KeyAllModule extends AbstractModule {

    public KeyAllModule(Hiru plugin) {
        super(plugin, "keyall");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new KeyAllCommands());
        logger.info("[Hiru] KeyAllModule loaded.");
    }
}
