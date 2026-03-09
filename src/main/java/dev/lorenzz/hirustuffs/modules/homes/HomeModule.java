package dev.lorenzz.hirustuffs.modules.homes;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.HomeManager;

public class HomeModule extends AbstractModule {

    private HomeManager homeManager;

    public HomeModule(Hiru plugin) {
        super(plugin, "homes");
    }

    @Override
    public void onEnable() {
        homeManager = new HomeManager(plugin);
        homeManager.init();
        plugin.getCommandManager().register(new HomeCommands());
        logger.info("[Hiru] HomeModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (homeManager != null) homeManager.shutdown();
    }
}
