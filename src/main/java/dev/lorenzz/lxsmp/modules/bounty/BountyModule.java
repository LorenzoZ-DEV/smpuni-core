package dev.lorenzz.lxsmp.modules.bounty;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.BountyManager;

public class BountyModule extends AbstractModule {

    private BountyManager bountyManager;

    public BountyModule(LXSMP plugin) {
        super(plugin, "bounty");
    }

    @Override
    public void onEnable() {
        bountyManager = new BountyManager(plugin);
        bountyManager.init();
        plugin.getCommandManager().register(new BountyCommands());
        plugin.getCommandManager().register(new BountiesCommand());
        registerListener(new BountyListener(plugin));
        logger.info("[Solum] BountyModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (bountyManager != null) bountyManager.shutdown();
    }
}
