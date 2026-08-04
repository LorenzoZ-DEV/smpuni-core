package dev.lorenzz.lxsmp.modules.statswipe;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class StatsWipeModule extends AbstractModule {

    public StatsWipeModule(LXSMP plugin) {
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
