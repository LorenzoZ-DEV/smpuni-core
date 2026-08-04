package dev.lorenzz.lxsmp.modules.sell;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class SellModule extends AbstractModule {

    public SellModule(LXSMP plugin) {
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
