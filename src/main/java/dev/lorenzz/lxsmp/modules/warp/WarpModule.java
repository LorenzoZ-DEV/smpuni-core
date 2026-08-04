package dev.lorenzz.lxsmp.modules.warp;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.WarpManager;

public class WarpModule extends AbstractModule {

    private WarpManager warpManager;

    public WarpModule(LXSMP plugin) {
        super(plugin, "warp");
    }

    @Override
    public void onEnable() {
        warpManager = new WarpManager(plugin);
        warpManager.init();
        plugin.getCommandManager().register(new WarpCommands());
        logger.info("[Solum] WarpModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (warpManager != null) warpManager.shutdown();
    }
}
