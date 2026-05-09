package dev.lorenzz.solum.modules.settings;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.SettingsManager;

public class SettingsModule extends AbstractModule {

    private SettingsManager settingsManager;

    public SettingsModule(Solum plugin) {
        super(plugin, "settings");
    }

    @Override
    public void onEnable() {
        settingsManager = new SettingsManager(plugin);
        settingsManager.init();
        plugin.getCommandManager().register(new SettingsCommands());
        logger.info("[Solum] SettingsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (settingsManager != null) settingsManager.shutdown();
    }
}
