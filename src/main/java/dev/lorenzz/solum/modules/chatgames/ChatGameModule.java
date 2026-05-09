package dev.lorenzz.solum.modules.chatgames;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import org.bukkit.Bukkit;

public class ChatGameModule extends AbstractModule {

    private ChatGameManager chatGameManager;

    public ChatGameModule(Solum plugin) {
        super(plugin, "chatgames");
    }

    @Override
    public void onEnable() {
        chatGameManager = new ChatGameManager(plugin);
        chatGameManager.start();

        ChatGameListener listener = new ChatGameListener(chatGameManager);
        // Use both AbstractModule's registerListener AND direct Bukkit registration
        // to ensure the listener is definitely registered
        registerListener(listener);
        logger.info("[Solum] ChatGameModule: listener registered via AbstractModule");

        plugin.getCommandManager().register(new ChatGameCommands(chatGameManager));

        logger.info("[Solum] ChatGameModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (chatGameManager != null) {
            chatGameManager.stop();
        }
    }

    public ChatGameManager getChatGameManager() {
        return chatGameManager;
    }
}
