package dev.lorenzz.solum.modules.spawn;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.SpawnManager;

public class SpawnModule extends AbstractModule {

    private SpawnManager spawnManager;

    public SpawnModule(Solum plugin) {
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
