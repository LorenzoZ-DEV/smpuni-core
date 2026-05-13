package dev.lorenzz.solum.modules.scoreboard;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class ScoreboardModule extends AbstractModule {

    private ScoreboardManager scoreboardManager;

    public ScoreboardModule(Solum plugin) {
        super(plugin, "scoreboard");
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    @Override
    public void onEnable() {
        scoreboardManager = new ScoreboardManager(plugin);
        scoreboardManager.start();
        registerListener(new ScoreboardListener(scoreboardManager));
        logger.info("[Solum] ScoreboardModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (scoreboardManager != null) {
            scoreboardManager.stop();
            scoreboardManager = null;
        }
    }

    @Override
    public void onReload() {
        if (scoreboardManager != null) {
            scoreboardManager.reloadConfig();
            scoreboardManager.stop();
            scoreboardManager.start();
        }
    }
}

