package dev.lorenzz.solum.modules.shop;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.ShopManager;

public class ShopModule extends AbstractModule {

    private ShopManager shopManager;

    public ShopModule(Solum plugin) {
        super(plugin, "shop");
    }

    @Override
    public void onEnable() {
        shopManager = new ShopManager(plugin);
        shopManager.init();
        plugin.getCommandManager().register(new ShopCommands());
        logger.info("[Solum] ShopModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
