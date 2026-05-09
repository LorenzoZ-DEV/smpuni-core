package dev.lorenzz.solum.modules.teams;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.TeamsManager;

public class TeamsModule extends AbstractModule {

    private TeamsManager teamsManager;

    public TeamsModule(Solum plugin) {
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
