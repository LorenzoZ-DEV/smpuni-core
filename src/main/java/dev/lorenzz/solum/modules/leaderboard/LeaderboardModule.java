package dev.lorenzz.solum.modules.leaderboard;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class LeaderboardModule extends AbstractModule {

    public LeaderboardModule(Solum plugin) {
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
