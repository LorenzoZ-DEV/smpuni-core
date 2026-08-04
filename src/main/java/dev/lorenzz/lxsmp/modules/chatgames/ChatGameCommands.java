package dev.lorenzz.lxsmp.modules.chatgames;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.command.CommandSender;

@CommandAlias("chatgame")
@CommandPermission("solum.admin")
@Description("Force start a chat game")
public class ChatGameCommands extends BaseCommand {

    private final ChatGameManager manager;

    public ChatGameCommands(ChatGameManager manager) {
        this.manager = manager;
    }

    @Default
    public void chatgame(CommandSender sender) {
        manager.forceStart();
        Messages.send(sender, "&aChat game started!");
    }
}

