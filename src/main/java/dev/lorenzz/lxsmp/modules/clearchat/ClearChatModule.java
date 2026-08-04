package dev.lorenzz.lxsmp.modules.clearchat;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class ClearChatModule extends AbstractModule {

    public ClearChatModule(LXSMP plugin) {
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
