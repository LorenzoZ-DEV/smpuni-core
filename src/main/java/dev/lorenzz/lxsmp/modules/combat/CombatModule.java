package dev.lorenzz.lxsmp.modules.combat;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class CombatModule extends AbstractModule {

    private static CombatModule INSTANCE;
    private CombatManager combatManager;

    public CombatModule(LXSMP plugin) {
        super(plugin, "combat");
    }

    public static CombatModule getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        combatManager = new CombatManager(plugin);
        registerListener(new CombatListener());
        plugin.getCommandManager().register(new CombatCommand());
        logger.info("[Solum] CombatModule loaded — " + combatManager.getCombatDurationSeconds() + "s combat tag.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (combatManager != null) {
            combatManager.shutdown();
        }
        combatManager = null;
        INSTANCE = null;
    }

    @Override
    public void onReload() {
        if (combatManager != null) {
            combatManager.reload();
        }
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }
}
