package dev.lorenzz.lxsmp.modules.media;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class MediaModule extends AbstractModule {

    public MediaModule(LXSMP plugin) {
        super(plugin, "media");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new MediaCommands());
        logger.info("[Solum] MediaModule loaded.");
    }
}
