package dev.lorenzz.hirustuffs.modules.broadcast;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class BroadcastModule extends AbstractModule {

    public BroadcastModule(Hiru plugin) {
        super(plugin, "broadcast");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new BroadcastCommand());
        logger.info("[Hiru] BroadcastModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
