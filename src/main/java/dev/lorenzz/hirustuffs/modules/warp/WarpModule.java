package dev.lorenzz.hirustuffs.modules.warp;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.WarpManager;

public class WarpModule extends AbstractModule {

    private WarpManager warpManager;

    public WarpModule(Hiru plugin) {
        super(plugin, "warp");
    }

    @Override
    public void onEnable() {
        warpManager = new WarpManager(plugin);
        warpManager.init();
        plugin.getCommandManager().register(new WarpCommands());
        logger.info("[Hiru] WarpModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (warpManager != null) warpManager.shutdown();
    }
}
