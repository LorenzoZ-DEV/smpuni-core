package dev.lorenzz.hirustuffs.modules.changelog;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class ChangelogModule extends AbstractModule {

    private ChangelogManager changelogManager;

    public ChangelogModule(Hiru plugin) {
        super(plugin, "changelog");
    }

    @Override
    public void onEnable() {
        changelogManager = new ChangelogManager(plugin);
        changelogManager.init();

        plugin.getCommandManager().register(new ChangelogCommands());
        plugin.getCommandManager().register(new ChangelogAdminCommands());

        logger.info("[Hiru] ChangelogModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (changelogManager != null) changelogManager.shutdown();
    }
}
