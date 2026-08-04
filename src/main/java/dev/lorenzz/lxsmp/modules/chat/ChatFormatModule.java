package dev.lorenzz.lxsmp.modules.chat;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class ChatFormatModule extends AbstractModule {

    public ChatFormatModule(LXSMP plugin) {
        super(plugin, "chat-format");
    }

    @Override
    public void onEnable() {
        registerListener(new ChatFormatListener(plugin));
        logger.info("[Solum] ChatFormatModule loaded.");
    }
}

