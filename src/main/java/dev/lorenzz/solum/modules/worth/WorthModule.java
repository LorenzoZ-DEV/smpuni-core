package dev.lorenzz.solum.modules.worth;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.WorthManager;

public class WorthModule extends AbstractModule {

    private WorthManager worthManager;

    public WorthModule(Solum plugin) {
        super(plugin, "worth");
    }

    @Override
    public void onEnable() {
        worthManager = new WorthManager(plugin);
        worthManager.init();
        plugin.getCommandManager().register(new WorthCommands());
        logger.info("[Solum] WorthModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
