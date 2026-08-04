package dev.lorenzz.lxsmp.modules.homes;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.HomeManager;

public class HomeModule extends AbstractModule {

    private HomeManager homeManager;

    public HomeModule(LXSMP plugin) {
        super(plugin, "homes");
    }

    @Override
    public void onEnable() {
        homeManager = new HomeManager(plugin);
        homeManager.init();
        plugin.getCommandManager().register(new HomeCommands());
        plugin.getCommandManager().register(new HomesCommand());
        plugin.getCommandManager().register(new SetHomeCommand());
        plugin.getCommandManager().register(new DelHomeCommand());
        logger.info("[Solum] HomeModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (homeManager != null) homeManager.shutdown();
    }
}
