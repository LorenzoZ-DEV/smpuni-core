package dev.lorenzz.hirustuffs.modules.worth;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.managers.WorthManager;

public class WorthModule extends AbstractModule {

    private WorthManager worthManager;

    public WorthModule(Hiru plugin) {
        super(plugin, "worth");
    }

    @Override
    public void onEnable() {
        worthManager = new WorthManager(plugin);
        worthManager.init();
        plugin.getCommandManager().register(new WorthCommands());
        logger.info("[Hiru] WorthModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
