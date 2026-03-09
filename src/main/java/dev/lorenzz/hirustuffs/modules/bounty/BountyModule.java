package dev.lorenzz.hirustuffs.modules.bounty;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.BountyManager;

public class BountyModule extends AbstractModule {

    private BountyManager bountyManager;

    public BountyModule(Hiru plugin) {
        super(plugin, "bounty");
    }

    @Override
    public void onEnable() {
        bountyManager = new BountyManager(plugin);
        bountyManager.init();
        plugin.getCommandManager().register(new BountyCommands());
        registerListener(new BountyListener(plugin));
        logger.info("[Hiru] BountyModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (bountyManager != null) bountyManager.shutdown();
    }
}
