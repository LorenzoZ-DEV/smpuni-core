package dev.lorenzz.solum.modules.shards;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.ShardManager;

public class ShardsModule extends AbstractModule {

    private ShardManager shardManager;

    public ShardsModule(Solum plugin) {
        super(plugin, "shards");
    }

    @Override
    public void onEnable() {
        shardManager = new ShardManager(plugin);
        shardManager.init();

        registerListener(new ShardsListener(plugin));
        plugin.getCommandManager().register(new ShardsCommand());
        plugin.getCommandManager().register(new ShardAdminCommands());

        logger.info("[Solum] ShardsModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (shardManager != null) shardManager.shutdown();
    }
}
