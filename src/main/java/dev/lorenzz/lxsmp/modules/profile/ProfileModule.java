package dev.lorenzz.lxsmp.modules.profile;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class ProfileModule extends AbstractModule {

    public ProfileModule(LXSMP plugin) {
        super(plugin, "profile");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new ProfileCommands());
        logger.info("[Solum] ProfileModule loaded.");
    }
}
