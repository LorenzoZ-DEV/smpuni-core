package dev.lorenzz.hirustuffs.modules.request;
import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
public class RequestCommand {
    private final RequestManager requestManager;
    public RequestCommand(RequestManager requestManager) {
        this.requestManager = requestManager;
    }
    @Command(name = "request", aliases = {"helpop", "req"}, description = "Send a request to the staff team", async = false)
    @Permission("hiru.request")
    public void request(@Sender Player player, @Combined @Named("message") String message) {
        if (message == null || message.trim().isEmpty()) {
            Messages.send(player, "&#FF4444Usage: &f/request <message>");
            return;
        }
        boolean submitted = requestManager.submitRequest(player, message);
        if (submitted) {
            int staffOnline = requestManager.getOnlineStaffCount();
            String separator = "&8&m                                                    ";
            Messages.sendRaw(player, "");
            Messages.sendRaw(player, separator);
            Messages.sendRaw(player, "&#4FC3F7&l  \u2709 REQUEST SENT");
            Messages.sendRaw(player, "");
            Messages.sendRaw(player, "&#8C8C8C  Message: &#E0E0E0" + message);
            Messages.sendRaw(player, "");
            if (staffOnline > 0) {
                Messages.sendRaw(player, "&#55FF55  " + staffOnline + " staff member" + (staffOnline > 1 ? "s" : "") + " notified.");
            } else {
                Messages.sendRaw(player, "&#FF9800  No staff members are currently online.");
                Messages.sendRaw(player, "&#8C8C8C  Your request has been logged and will be reviewed.");
            }
            Messages.sendRaw(player, separator);
            Messages.sendRaw(player, "");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }
    }
}
