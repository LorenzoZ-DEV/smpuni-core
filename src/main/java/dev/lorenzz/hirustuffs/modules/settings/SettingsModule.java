package dev.lorenzz.hirustuffs.modules.settings;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.SettingsManager;

public class SettingsModule extends AbstractModule {

    private SettingsManager settingsManager;

    public SettingsModule(Hiru plugin) {
        super(plugin, "settings");
    }

    @Override
    public void onEnable() {
        settingsManager = new SettingsManager(plugin);
        settingsManager.init();
        plugin.getCommandManager().register(new SettingsCommands());
        logger.info("[Hiru] SettingsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (settingsManager != null) settingsManager.shutdown();
    }
}
