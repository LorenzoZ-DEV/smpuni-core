package dev.lorenzz.hirustuffs.modules.shards;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.ShardManager;

public class ShardsModule extends AbstractModule {

    private ShardManager shardManager;

    public ShardsModule(Hiru plugin) {
        super(plugin, "shards");
    }

    @Override
    public void onEnable() {
        shardManager = new ShardManager(plugin);
        shardManager.init();

        registerListener(new ShardsListener(plugin));
        plugin.getCommandManager().register(new ShardsCommand());
        plugin.getCommandManager().register(new ShardAdminCommands());

        logger.info("[Hiru] ShardsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (shardManager != null) shardManager.shutdown();
    }
}
