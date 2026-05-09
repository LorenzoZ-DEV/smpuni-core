package dev.lorenzz.solum.modules.clearchat;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class ClearChatModule extends AbstractModule {

    public ClearChatModule(Solum plugin) {
        super(plugin, "clearchat");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new ClearChatCommand());
        logger.info("[Solum] ClearChatModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
