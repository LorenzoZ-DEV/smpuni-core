package dev.lorenzz.lxsmp.modules.shop;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.ShopManager;

public class ShopModule extends AbstractModule {

    private ShopManager shopManager;

    public ShopModule(LXSMP plugin) {
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
