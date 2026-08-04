package dev.lorenzz.lxsmp.modules.spawn;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.SpawnManager;

public class SpawnModule extends AbstractModule {

    private SpawnManager spawnManager;

    public SpawnModule(LXSMP plugin) {
        super(plugin, "spawn");
    }

    @Override
    public void onEnable() {
        spawnManager = new SpawnManager(plugin);
        spawnManager.init();
        plugin.getCommandManager().register(new SpawnCommands());
        logger.info("[Solum] SpawnModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (spawnManager != null) spawnManager.shutdown();
    }
}
