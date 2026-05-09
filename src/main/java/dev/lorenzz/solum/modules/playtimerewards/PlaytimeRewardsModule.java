package dev.lorenzz.solum.modules.playtimerewards;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.PlaytimeRewardsManager;

public class PlaytimeRewardsModule extends AbstractModule {

    private PlaytimeRewardsManager rewardsManager;

    public PlaytimeRewardsModule(Solum plugin) {
        super(plugin, "playtime-rewards");
    }

    @Override
    public void onEnable() {
        rewardsManager = new PlaytimeRewardsManager(plugin);
        rewardsManager.init();

        plugin.getCommandManager().register(new PlaytimeRewardsCommand());
        registerListener(new PlaytimeRewardsListener(plugin));

        logger.info("[Solum] PlaytimeRewardsModule loaded — " + rewardsManager.getMilestones().size() + " milestones.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (rewardsManager != null) rewardsManager.shutdown();
    }
}
