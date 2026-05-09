package dev.lorenzz.solum.modules.changelog;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class ChangelogModule extends AbstractModule {

    private ChangelogManager changelogManager;

    public ChangelogModule(Solum plugin) {
        super(plugin, "changelog");
    }

    @Override
    public void onEnable() {
        changelogManager = new ChangelogManager(plugin);
        changelogManager.init();

        plugin.getCommandManager().register(new ChangelogCommands());
        plugin.getCommandManager().register(new ChangelogAdminCommands());

        logger.info("[Solum] ChangelogModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (changelogManager != null) changelogManager.shutdown();
    }
}
