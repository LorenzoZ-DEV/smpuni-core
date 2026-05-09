package dev.lorenzz.solum.modules.tablist;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener that wires {@link TablistManager#attach(org.bukkit.entity.Player)}
 * and {@link TablistManager#detach(org.bukkit.entity.Player)} to the player
 * connection lifecycle, then triggers a global refresh so other players see
 * the new sort/team state immediately.
 */
public class TablistListener implements Listener {

    private final TablistManager manager;

    public TablistListener(TablistManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        manager.attach(event.getPlayer());
        manager.refreshAll();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.detach(event.getPlayer());
        manager.refreshAll();
    }
}

