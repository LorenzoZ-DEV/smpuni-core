package dev.lorenzz.solum.modules.statswipe;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class StatsWipeModule extends AbstractModule {

    public StatsWipeModule(Solum plugin) {
        super(plugin, "stats");
    }

    @Override
    public String getName() {
        return "statswipe";
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfigFile().getBoolean("modules.stats", true);
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new StatsWipeCommands());
        logger.info("[Solum] StatsWipeModule loaded.");
    }
}
