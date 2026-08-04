package dev.lorenzz.lxsmp.modules.auctionhouse;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class AuctionHouseModule extends AbstractModule {

    private AuctionHouseManager auctionHouseManager;
    private AuctionHouseGUI auctionHouseGui;

    public AuctionHouseModule(LXSMP plugin) {
        super(plugin, "auctionhouse");
    }

    public AuctionHouseManager getAuctionHouseManager() {
        return auctionHouseManager;
    }

    @Override
    public void onEnable() {
        auctionHouseManager = new AuctionHouseManager(plugin);
        auctionHouseManager.init();
        auctionHouseGui = new AuctionHouseGUI(plugin, auctionHouseManager);
        plugin.getCommandManager().register(new AuctionHouseCommands(plugin, auctionHouseManager, auctionHouseGui));
        logger.info("[Solum] AuctionHouseModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (auctionHouseManager != null) {
            auctionHouseManager.shutdown();
            auctionHouseManager = null;
        }
        auctionHouseGui = null;
    }
}

