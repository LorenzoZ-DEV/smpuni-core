package dev.lorenzz.hirustuffs.modules.playtimerewards;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.PlaytimeRewardsManager;

public class PlaytimeRewardsModule extends AbstractModule {

    private PlaytimeRewardsManager rewardsManager;

    public PlaytimeRewardsModule(Hiru plugin) {
        super(plugin, "playtime-rewards");
    }

    @Override
    public void onEnable() {
        rewardsManager = new PlaytimeRewardsManager(plugin);
        rewardsManager.init();

        plugin.getCommandManager().register(new PlaytimeRewardsCommand());
        registerListener(new PlaytimeRewardsListener(plugin));

        logger.info("[Hiru] PlaytimeRewardsModule loaded — " + rewardsManager.getMilestones().size() + " milestones.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (rewardsManager != null) rewardsManager.shutdown();
    }
}
