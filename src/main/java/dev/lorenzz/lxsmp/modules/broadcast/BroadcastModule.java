package dev.lorenzz.lxsmp.modules.broadcast;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class BroadcastModule extends AbstractModule {

    public BroadcastModule(LXSMP plugin) {
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
