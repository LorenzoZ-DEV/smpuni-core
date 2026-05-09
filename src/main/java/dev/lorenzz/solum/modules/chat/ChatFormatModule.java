package dev.lorenzz.solum.modules.chat;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class ChatFormatModule extends AbstractModule {

    public ChatFormatModule(Solum plugin) {
        super(plugin, "chat-format");
    }

    @Override
    public void onEnable() {
        registerListener(new ChatFormatListener(plugin));
        logger.info("[Solum] ChatFormatModule loaded.");
    }
}

