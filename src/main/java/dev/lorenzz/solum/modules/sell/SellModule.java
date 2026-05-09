package dev.lorenzz.solum.modules.sell;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class SellModule extends AbstractModule {

    public SellModule(Solum plugin) {
        super(plugin, "sell");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new SellCommands());
        registerListener(new SellMenuListener());
        logger.info("[Solum] SellModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
