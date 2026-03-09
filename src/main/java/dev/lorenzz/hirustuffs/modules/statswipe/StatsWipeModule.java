package dev.lorenzz.hirustuffs.modules.statswipe;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class StatsWipeModule extends AbstractModule {

    public StatsWipeModule(Hiru plugin) {
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
        logger.info("[Hiru] StatsWipeModule loaded.");
    }
}
