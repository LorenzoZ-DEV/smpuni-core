package dev.lorenzz.solum.modules.bounty;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.BountyManager;

public class BountyModule extends AbstractModule {

    private BountyManager bountyManager;

    public BountyModule(Solum plugin) {
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
