package dev.lorenzz.lxsmp.modules.doublejump;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class DoubleJumpModule extends AbstractModule {

    private DoubleJumpListener listener;

    public DoubleJumpModule(LXSMP plugin) {
        super(plugin, "double-jump");
    }

    @Override
    public void onEnable() {
        listener = new DoubleJumpListener();
        registerListener(listener);
        logger.info("[Solum] DoubleJumpModule loaded.");
    }

    @Override
    public void onDisable() {
        if (listener != null) listener.cleanup();
        super.onDisable();
    }

    @Override
    public void onReload() {
        if (listener != null) listener.refreshConfig();
        logger.info("[Solum] DoubleJumpModule reloaded.");
    }
}
