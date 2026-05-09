package dev.lorenzz.solum.modules.duel;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.DuelManager;

public class DuelModule extends AbstractModule {

    private DuelManager duelManager;

    public DuelModule(Solum plugin) {
        super(plugin, "duel");
    }

    @Override
    public void onEnable() {
        duelManager = new DuelManager(plugin);
        duelManager.init();
        plugin.getCommandManager().register(new DuelCommands());
        plugin.getCommandManager().register(new DuelAcceptCommand());
        plugin.getCommandManager().register(new DuelDenyCommand());
        plugin.getCommandManager().register(new DuelAdminCommands());
        registerListener(new DuelListener(plugin));
        logger.info("[Solum] DuelModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (duelManager != null) duelManager.shutdown();
    }
}
