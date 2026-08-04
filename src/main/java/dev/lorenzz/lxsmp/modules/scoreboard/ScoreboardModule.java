package dev.lorenzz.lxsmp.modules.scoreboard;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class ScoreboardModule extends AbstractModule {

    private ScoreboardManager scoreboardManager;

    public ScoreboardModule(LXSMP plugin) {
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

