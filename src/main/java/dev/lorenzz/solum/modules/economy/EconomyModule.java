package dev.lorenzz.solum.modules.economy;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.EconomyManager;

public class EconomyModule extends AbstractModule {

    private EconomyManager economyManager;

    public EconomyModule(Solum plugin) {
        super(plugin, "economy");
    }

    @Override
    public void onEnable() {
        economyManager = new EconomyManager(plugin);
        economyManager.init();

        plugin.getCommandManager().register(new EconomyCommands());
        plugin.getCommandManager().register(new PayCommand());
        plugin.getCommandManager().register(new BalTopCommand());
        plugin.getCommandManager().register(new EcoAdminCommands());

        logger.info("[Solum] EconomyModule loaded - Vault hooked.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (economyManager != null) economyManager.shutdown();
    }

    public EconomyManager getEconomyManager() { return economyManager; }
}
