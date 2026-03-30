package dev.lorenzz.hirustuffs.modules.chatgames;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.command.CommandSender;

public class ChatGameCommands {

    private final ChatGameManager manager;

    public ChatGameCommands(ChatGameManager manager) {
        this.manager = manager;
    }

    @Command(name = "chatgame", description = "Force start a chat game")
    @Permission("hiru.admin")
    public void chatgame(@Sender CommandSender sender) {
        manager.forceStart();
        Messages.send(sender, "&aChat game started!");
    }
}
