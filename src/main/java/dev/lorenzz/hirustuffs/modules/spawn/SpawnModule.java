package dev.lorenzz.hirustuffs.modules.spawn;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.SpawnManager;

public class SpawnModule extends AbstractModule {

    private SpawnManager spawnManager;

    public SpawnModule(Hiru plugin) {
        super(plugin, "spawn");
    }

    @Override
    public void onEnable() {
        spawnManager = new SpawnManager(plugin);
        spawnManager.init();
        plugin.getCommandManager().register(new SpawnCommands());
        logger.info("[Hiru] SpawnModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (spawnManager != null) spawnManager.shutdown();
    }
}
