package dev.lorenzz.lxsmp.modules.tpa;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.TPAManager;

public class TPAModule extends AbstractModule {

    private TPAManager tpaManager;

    public TPAModule(LXSMP plugin) {
        super(plugin, "tpa");
    }

    @Override
    public void onEnable() {
        tpaManager = new TPAManager(plugin);
        plugin.getCommandManager().register(new TPACommands());
        logger.info("[Solum] TPAModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (tpaManager != null) tpaManager.shutdown();
    }
}
