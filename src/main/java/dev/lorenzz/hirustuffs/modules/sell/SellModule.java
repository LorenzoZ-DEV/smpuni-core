package dev.lorenzz.hirustuffs.modules.sell;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class SellModule extends AbstractModule {

    public SellModule(Hiru plugin) {
        super(plugin, "sell");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new SellCommands());
        registerListener(new SellMenuListener());
        logger.info("[Hiru] SellModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
