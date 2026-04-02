package dev.lorenzz.hirustuffs.modules.easter;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class EasterModule extends AbstractModule {

    private EasterManager easterManager;

    public EasterModule(Hiru plugin) {
        super(plugin, "easter");
    }

    @Override
    public void onEnable() {
        easterManager = new EasterManager(plugin);

        registerListener(new EasterListener(easterManager, plugin));

        plugin.getCommandManager().register(new EasterCommands(easterManager));

        logger.info("[Hiru] EasterModule loaded.");
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
