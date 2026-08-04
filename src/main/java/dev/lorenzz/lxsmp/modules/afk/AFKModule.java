package dev.lorenzz.lxsmp.modules.afk;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AFKManager;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class AFKModule extends AbstractModule {

    private AFKManager afkManager;

    public AFKModule(LXSMP plugin) {
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
