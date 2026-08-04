package dev.lorenzz.lxsmp.modules.stats;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.StatsManager;

public class StatsModule extends AbstractModule {

    private StatsManager statsManager;

    public StatsModule(LXSMP plugin) {
        super(plugin, "stats");
    }

    @Override
    public void onEnable() {
        statsManager = new StatsManager(plugin);
        statsManager.init();
        logger.info("[Solum] StatsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (statsManager != null) statsManager.shutdown();
    }
}
