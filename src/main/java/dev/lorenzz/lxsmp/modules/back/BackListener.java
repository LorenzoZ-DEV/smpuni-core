package dev.lorenzz.lxsmp.modules.back;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class BackListener implements Listener {

    private final BackManager backManager;

    public BackListener(BackManager backManager) {
        this.backManager = backManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        backManager.setDeathLocation(player.getUniqueId(), player.getLocation());

        // Send clickable /back message after a short delay
        player.getServer().getScheduler().runTaskLater(
                LXSMP.getInstance(), () -> {
                    if (!player.isOnline()) return;

                    Component message = Messages.colorComponent("&#8C8C8C☠ You died! ")
                            .append(Messages.colorComponent("&#55FF55&l[CLICK HERE]")
                                    .clickEvent(ClickEvent.runCommand("/back"))
                                    .hoverEvent(HoverEvent.showText(
                                            Messages.colorComponent("&#FFD700Click to teleport back to your death location"))))
                            .append(Messages.colorComponent(" &#8C8C8Cor type &#E0E0E0/back &#8C8C8Cto return to your death location."));

                    player.sendMessage(message);
                }, 20L); // 1 second delay so it shows after respawn
    }
}
