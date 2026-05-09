package dev.lorenzz.solum.modules.tools;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class AmethystToolsModule extends AbstractModule {

    private AmethystToolsListener listener;

    public AmethystToolsModule(Solum plugin) {
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
