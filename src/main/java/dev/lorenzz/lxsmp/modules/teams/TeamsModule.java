package dev.lorenzz.lxsmp.modules.teams;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.TeamsManager;

public class TeamsModule extends AbstractModule {

    private TeamsManager teamsManager;

    public TeamsModule(LXSMP plugin) {
        super(plugin, "teams");
    }

    @Override
    public void onEnable() {
        teamsManager = new TeamsManager(plugin);
        teamsManager.init();
        plugin.getCommandManager().register(new TeamsCommands());
        registerListener(new TeamsListener(plugin));
        logger.info("[Solum] TeamsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (teamsManager != null) teamsManager.shutdown();
    }
}
