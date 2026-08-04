package dev.lorenzz.lxsmp.modules.shards;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.ShardManager;

public class ShardsModule extends AbstractModule {

    private ShardManager shardManager;

    public ShardsModule(LXSMP plugin) {
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
