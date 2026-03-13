package dev.lorenzz.hirustuffs.modules.clearchat;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class ClearChatModule extends AbstractModule {

    public ClearChatModule(Hiru plugin) {
        super(plugin, "clearchat");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new ClearChatCommand());
        logger.info("[Hiru] ClearChatModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
