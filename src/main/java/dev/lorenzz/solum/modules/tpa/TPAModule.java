package dev.lorenzz.solum.modules.tpa;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.TPAManager;

public class TPAModule extends AbstractModule {

    private TPAManager tpaManager;

    public TPAModule(Solum plugin) {
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
