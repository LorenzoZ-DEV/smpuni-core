package dev.lorenzz.lxsmp.modules.leaderboard;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class LeaderboardModule extends AbstractModule {

    public LeaderboardModule(LXSMP plugin) {
        super(plugin, "leaderboard");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new LeaderboardCommands());
        logger.info("[Solum] LeaderboardModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
