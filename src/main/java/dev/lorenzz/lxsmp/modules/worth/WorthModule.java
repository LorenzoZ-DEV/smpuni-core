package dev.lorenzz.lxsmp.modules.worth;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.managers.WorthManager;

public class WorthModule extends AbstractModule {

    private WorthManager worthManager;

    public WorthModule(LXSMP plugin) {
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
