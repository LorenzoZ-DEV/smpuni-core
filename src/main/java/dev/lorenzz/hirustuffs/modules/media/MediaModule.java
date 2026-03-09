package dev.lorenzz.hirustuffs.modules.media;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class MediaModule extends AbstractModule {

    public MediaModule(Hiru plugin) {
        super(plugin, "media");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new MediaCommands());
        logger.info("[Hiru] MediaModule loaded.");
    }
}
