package dev.lorenzz.hirustuffs.modules.tools;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class AmethystToolsModule extends AbstractModule {

    private AmethystToolsListener listener;

    public AmethystToolsModule(Hiru plugin) {
        super(plugin, "tools");
    }

    @Override
    public void onEnable() {
        listener = new AmethystToolsListener(plugin);
        registerListener(listener);
        plugin.getCommandManager().register(new GiveAmethystCommand());
        logger.info("[Hiru] AmethystToolsModule loaded.");
    }

    public AmethystToolsListener getListener() {
        return listener;
    }
}
