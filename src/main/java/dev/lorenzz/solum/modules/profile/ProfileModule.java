package dev.lorenzz.solum.modules.profile;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class ProfileModule extends AbstractModule {

    public ProfileModule(Solum plugin) {
        super(plugin, "profile");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new ProfileCommands());
        logger.info("[Solum] ProfileModule loaded.");
    }
}
