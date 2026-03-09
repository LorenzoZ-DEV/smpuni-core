package dev.lorenzz.hirustuffs.modules.teams;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.TeamsManager;

public class TeamsModule extends AbstractModule {

    private TeamsManager teamsManager;

    public TeamsModule(Hiru plugin) {
        super(plugin, "teams");
    }

    @Override
    public void onEnable() {
        teamsManager = new TeamsManager(plugin);
        teamsManager.init();
        plugin.getCommandManager().register(new TeamsCommands());
        registerListener(new TeamsListener(plugin));
        logger.info("[Hiru] TeamsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (teamsManager != null) teamsManager.shutdown();
    }
}
