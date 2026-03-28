package dev.lorenzz.hirustuffs.modules.duel;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.DuelManager;

public class DuelModule extends AbstractModule {

    private DuelManager duelManager;

    public DuelModule(Hiru plugin) {
        super(plugin, "duel");
    }

    @Override
    public void onEnable() {
        duelManager = new DuelManager(plugin);
        duelManager.init();
        plugin.getCommandManager().register(new DuelCommands());
        plugin.getCommandManager().register(new DuelAdminCommands());
        registerListener(new DuelListener(plugin));
        logger.info("[Hiru] DuelModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (duelManager != null) duelManager.shutdown();
    }
}
