package dev.lorenzz.solum.modules.chatgames;

import dev.lorenzz.solum.Solum;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("deprecation")
public class ChatGameListener implements Listener {

    private final ChatGameManager manager;
    private final Solum plugin;

    public ChatGameListener(ChatGameManager manager) {
        this.manager = manager;
        this.plugin = Solum.getInstance();
        plugin.getLogger().info("[ChatGame] ChatGameListener registered!");
    }

    /**
     * Paper's modern AsyncChatEvent — tries this first.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onModernChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;

        ChatGameManager.ActiveGame game = manager.getActiveGame();
        if (game == null) return;

        Player player = event.getPlayer();
        String message;
        try {
            message = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        } catch (Exception e) {
            try {
                message = PlainTextComponentSerializer.plainText().serialize(event.message());
            } catch (Exception e2) {
                return;
            }
        }

        if (message == null || message.isEmpty()) return;

        plugin.getLogger().info("[ChatGame-Modern] " + player.getName() + " typed: '" + message
                + "' | Expected: '" + game.getAnswer() + "'");

        if (manager.isCorrectAnswer(message)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> manager.handleCorrectAnswer(player));
        }
    }

    /**
     * Legacy AsyncPlayerChatEvent — fallback for servers/plugins that
     * suppress or never fire the modern Paper event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;

        ChatGameManager.ActiveGame game = manager.getActiveGame();
        if (game == null) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message == null || message.isEmpty()) return;

        plugin.getLogger().info("[ChatGame-Legacy] " + player.getName() + " typed: '" + message
                + "' | Expected: '" + game.getAnswer() + "'");

        if (manager.isCorrectAnswer(message)) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> manager.handleCorrectAnswer(player));
        }
    }
}
