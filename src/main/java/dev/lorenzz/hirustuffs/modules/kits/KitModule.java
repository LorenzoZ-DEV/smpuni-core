package dev.lorenzz.hirustuffs.modules.kits;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class KitModule extends AbstractModule {

    private KitManager kitManager;
    private KitPlayerDataManager playerDataManager;

    public KitModule(Hiru plugin) {
        super(plugin, "kits");
    }

    @Override
    public void onEnable() {
        kitManager = new KitManager(plugin);
        kitManager.load();

        playerDataManager = new KitPlayerDataManager(plugin);
        playerDataManager.load();

        plugin.getCommandManager().register(new KitCommand());
        plugin.getCommandManager().register(new KitAdminCommand());

        registerListener(new KitListener(plugin));

        logger.info("[Hiru] KitModule loaded — " + kitManager.getKits().size() + " kits available.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (playerDataManager != null) playerDataManager.save();
    }

    public KitManager getKitManager() { return kitManager; }
    public KitPlayerDataManager getPlayerDataManager() { return playerDataManager; }
}
