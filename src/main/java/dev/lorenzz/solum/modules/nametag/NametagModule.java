package dev.lorenzz.solum.modules.nametag;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class NametagModule extends AbstractModule {

    private xyz.lorenzz.minenationsutils.nametags.NametagManager manager;

    public NametagModule(Solum plugin) {
        super(plugin, "nametag");
    }

    public xyz.lorenzz.minenationsutils.nametags.NametagManager getManager() {
        return manager;
    }

    @Override
    public void onEnable() {
        manager = new xyz.lorenzz.minenationsutils.nametags.NametagManager(plugin);
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

