package dev.lorenzz.lxsmp.modules.tools;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class AmethystToolsModule extends AbstractModule {

    private AmethystToolsListener listener;

    public AmethystToolsModule(LXSMP plugin) {
        super(plugin, "tools");
    }

    @Override
    public void onEnable() {
        listener = new AmethystToolsListener(plugin);
        registerListener(listener);
        plugin.getCommandManager().register(new GiveAmethystCommand());
        logger.info("[Solum] AmethystToolsModule loaded.");
    }

    public AmethystToolsListener getListener() {
        return listener;
    }
}
