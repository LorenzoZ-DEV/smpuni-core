package dev.lorenzz.solum.modules.media;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class MediaModule extends AbstractModule {

    public MediaModule(Solum plugin) {
        super(plugin, "media");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new MediaCommands());
        logger.info("[Solum] MediaModule loaded.");
    }
}
