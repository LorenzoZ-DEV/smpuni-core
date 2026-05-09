package dev.lorenzz.solum.modules.joinquit;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class JoinQuitModule extends AbstractModule {

    public JoinQuitModule(Solum plugin) {
        super(plugin, "join-quit");
    }

    @Override
    public void onEnable() {
        registerListener(new JoinQuitListener(plugin));
        logger.info("[Solum] JoinQuitModule loaded.");
    }
}

