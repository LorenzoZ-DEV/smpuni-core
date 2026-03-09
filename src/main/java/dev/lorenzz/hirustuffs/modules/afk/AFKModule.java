package dev.lorenzz.hirustuffs.modules.afk;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AFKManager;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class AFKModule extends AbstractModule {

    private AFKManager afkManager;

    public AFKModule(Hiru plugin) {
        super(plugin, "afk");
    }

    @Override
    public void onEnable() {
        afkManager = new AFKManager(plugin);
        afkManager.init();
        plugin.getCommandManager().register(new AFKCommand());
        logger.info("[Hiru] AFKModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (afkManager != null) afkManager.shutdown();
    }
}
