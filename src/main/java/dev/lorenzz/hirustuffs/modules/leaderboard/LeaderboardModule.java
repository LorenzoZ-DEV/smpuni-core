package dev.lorenzz.hirustuffs.modules.leaderboard;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class LeaderboardModule extends AbstractModule {

    public LeaderboardModule(Hiru plugin) {
        super(plugin, "leaderboard");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new LeaderboardCommands());
        logger.info("[Hiru] LeaderboardModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
