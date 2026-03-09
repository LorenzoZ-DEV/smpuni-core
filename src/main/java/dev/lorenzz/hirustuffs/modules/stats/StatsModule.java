package dev.lorenzz.hirustuffs.modules.stats;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.StatsManager;

public class StatsModule extends AbstractModule {

    private StatsManager statsManager;

    public StatsModule(Hiru plugin) {
        super(plugin, "stats");
    }

    @Override
    public void onEnable() {
        statsManager = new StatsManager(plugin);
        statsManager.init();
        logger.info("[Hiru] StatsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (statsManager != null) statsManager.shutdown();
    }
}
