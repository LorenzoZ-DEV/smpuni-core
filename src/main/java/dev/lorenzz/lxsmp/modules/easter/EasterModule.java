package dev.lorenzz.lxsmp.modules.easter;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class EasterModule extends AbstractModule {

    private EasterManager easterManager;

    public EasterModule(LXSMP plugin) {
        super(plugin, "easter");
    }

    @Override
    public void onEnable() {
        easterManager = new EasterManager(plugin);

        registerListener(new EasterListener(easterManager, plugin));

        plugin.getCommandManager().register(new EasterCommands(easterManager));

        logger.info("[Solum] EasterModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (easterManager != null) {
            easterManager.shutdown();
        }
    }

    public EasterManager getEasterManager() {
        return easterManager;
    }
}
