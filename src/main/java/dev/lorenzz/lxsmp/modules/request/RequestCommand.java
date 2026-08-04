package dev.lorenzz.lxsmp.modules.request;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.entity.Player;

@CommandAlias("request|helpop")
@CommandPermission("solum.request")
@Description("Send a request/message to online staff")
public class RequestCommand extends BaseCommand {

    private final RequestManager manager;
    private final LXSMP plugin = LXSMP.getInstance();

    public RequestCommand(RequestManager manager) {
        this.manager = manager;
    }

    @Default
    public void request(Player sender, String message) {
        if (message == null || message.trim().isEmpty()) {
            String usage = plugin.getConfigFile().getString("request.messages.usage",
                    "&#FF4444Usage: &#FFD700/request <message>");
            Messages.send(sender, usage);
            return;
        }

        if (manager.submitRequest(sender, message.trim())) {
            String confirmation = plugin.getConfigFile().getString("request.messages.submitted",
                    "&#55FF55Your request has been sent to the staff.");
            Messages.send(sender, confirmation);
        }
    }
}

