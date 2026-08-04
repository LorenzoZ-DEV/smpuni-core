package dev.lorenzz.lxsmp.modules.back;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class BackModule extends AbstractModule {

    private static BackModule INSTANCE;
    private BackManager backManager;

    public BackModule(LXSMP plugin) {
        super(plugin, "back");
    }

    public static BackModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        backManager = new BackManager(plugin);
        registerListener(new BackListener(backManager));
        plugin.getCommandManager().register(new BackCommand());
        logger.info("[Solum] BackModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        INSTANCE = null;
        backManager = null;
    }

    public BackManager getBackManager() {
        return backManager;
    }
}
