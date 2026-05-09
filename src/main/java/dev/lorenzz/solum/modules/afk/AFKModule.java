package dev.lorenzz.solum.modules.afk;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AFKManager;
import dev.lorenzz.solum.managers.AbstractModule;

public class AFKModule extends AbstractModule {

    private AFKManager afkManager;

    public AFKModule(Solum plugin) {
        super(plugin, "afk");
    }

    @Override
    public void onEnable() {
        afkManager = new AFKManager(plugin);
        afkManager.init();
        plugin.getCommandManager().register(new AFKCommand());
        plugin.getCommandManager().register(new SetAfkZoneCommand());
        plugin.getCommandManager().register(new RemoveAfkZoneCommand());
        plugin.getCommandManager().register(new AfkZonesListCommand());
        logger.info("[Solum] AFKModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (afkManager != null) afkManager.shutdown();
    }
}
