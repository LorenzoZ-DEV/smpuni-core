package dev.lorenzz.lxsmp.modules.nametag;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class NametagModule extends AbstractModule {

    private NametagManager manager;

    public NametagModule(LXSMP plugin) {
        super(plugin, "nametag");
    }

    public NametagManager getManager() {
        return manager;
    }

    @Override
    public void onEnable() {
        manager = new NametagManager(plugin);
        manager.start();
        registerListener(new NametagListener(manager));
        logger.info("[Solum] NametagModule loaded (TextDisplay below vanilla nametag).");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (manager != null) {
            manager.stop();
            manager = null;
        }
    }

    @Override
    public void onReload() {
        if (manager != null) manager.reload();
    }
}

