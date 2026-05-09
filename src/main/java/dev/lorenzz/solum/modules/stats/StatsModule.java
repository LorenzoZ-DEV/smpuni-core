package dev.lorenzz.solum.modules.stats;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.StatsManager;

public class StatsModule extends AbstractModule {

    private StatsManager statsManager;

    public StatsModule(Solum plugin) {
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
