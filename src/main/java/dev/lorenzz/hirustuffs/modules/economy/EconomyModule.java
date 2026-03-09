package dev.lorenzz.hirustuffs.modules.economy;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.EconomyManager;

public class EconomyModule extends AbstractModule {

    private EconomyManager economyManager;

    public EconomyModule(Hiru plugin) {
        super(plugin, "economy");
    }

    @Override
    public void onEnable() {
        economyManager = new EconomyManager(plugin);
        economyManager.init();

        plugin.getCommandManager().register(new EconomyCommands());

        logger.info("[Hiru] EconomyModule loaded - Vault hooked.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (economyManager != null) economyManager.shutdown();
    }

    public EconomyManager getEconomyManager() { return economyManager; }
}
