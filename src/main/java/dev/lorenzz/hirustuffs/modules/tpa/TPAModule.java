package dev.lorenzz.hirustuffs.modules.tpa;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.TPAManager;

public class TPAModule extends AbstractModule {

    private TPAManager tpaManager;

    public TPAModule(Hiru plugin) {
        super(plugin, "tpa");
    }

    @Override
    public void onEnable() {
        tpaManager = new TPAManager(plugin);
        plugin.getCommandManager().register(new TPACommands());
        logger.info("[Hiru] TPAModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (tpaManager != null) tpaManager.shutdown();
    }
}
