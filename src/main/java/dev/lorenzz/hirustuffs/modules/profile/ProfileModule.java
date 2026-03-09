package dev.lorenzz.hirustuffs.modules.profile;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class ProfileModule extends AbstractModule {

    public ProfileModule(Hiru plugin) {
        super(plugin, "profile");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new ProfileCommands());
        logger.info("[Hiru] ProfileModule loaded.");
    }
}
