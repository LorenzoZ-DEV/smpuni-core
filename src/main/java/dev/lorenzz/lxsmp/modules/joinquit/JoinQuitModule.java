package dev.lorenzz.lxsmp.modules.joinquit;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class JoinQuitModule extends AbstractModule {

    public JoinQuitModule(LXSMP plugin) {
        super(plugin, "join-quit");
    }

    @Override
    public void onEnable() {
        registerListener(new JoinQuitListener(plugin));
        logger.info("[Solum] JoinQuitModule loaded.");
    }
}

