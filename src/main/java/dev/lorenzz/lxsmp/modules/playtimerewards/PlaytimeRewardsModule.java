package dev.lorenzz.lxsmp.modules.playtimerewards;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.PlaytimeRewardsManager;

public class PlaytimeRewardsModule extends AbstractModule {

    private PlaytimeRewardsManager rewardsManager;

    public PlaytimeRewardsModule(LXSMP plugin) {
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
