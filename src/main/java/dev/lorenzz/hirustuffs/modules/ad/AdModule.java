package dev.lorenzz.hirustuffs.modules.ad;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class AdModule extends AbstractModule {

    public AdModule(Hiru plugin) {
        super(plugin, "ad");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new AdCommand());
        logger.info("[Hiru] AdModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
