package dev.lorenzz.lxsmp.modules.duel;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.DuelManager;

public class DuelModule extends AbstractModule {

    private DuelManager duelManager;

    public DuelModule(LXSMP plugin) {
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
