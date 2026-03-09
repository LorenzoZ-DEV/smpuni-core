package dev.lorenzz.hirustuffs.modules.shop;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.ShopManager;

public class ShopModule extends AbstractModule {

    private ShopManager shopManager;

    public ShopModule(Hiru plugin) {
        super(plugin, "shop");
    }

    @Override
    public void onEnable() {
        shopManager = new ShopManager(plugin);
        shopManager.init();
        plugin.getCommandManager().register(new ShopCommands());
        logger.info("[Hiru] ShopModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
