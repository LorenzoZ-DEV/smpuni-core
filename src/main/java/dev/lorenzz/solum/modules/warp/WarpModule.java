package dev.lorenzz.solum.modules.warp;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.WarpManager;

public class WarpModule extends AbstractModule {

    private WarpManager warpManager;

    public WarpModule(Solum plugin) {
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
