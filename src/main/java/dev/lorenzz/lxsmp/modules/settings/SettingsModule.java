package dev.lorenzz.lxsmp.modules.settings;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.SettingsManager;

public class SettingsModule extends AbstractModule {

    private SettingsManager settingsManager;

    public SettingsModule(LXSMP plugin) {
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
