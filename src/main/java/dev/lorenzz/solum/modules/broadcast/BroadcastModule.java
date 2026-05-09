package dev.lorenzz.solum.modules.broadcast;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class BroadcastModule extends AbstractModule {

    public BroadcastModule(Solum plugin) {
        super(plugin, "broadcast");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new BroadcastCommand());
        logger.info("[Solum] BroadcastModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
