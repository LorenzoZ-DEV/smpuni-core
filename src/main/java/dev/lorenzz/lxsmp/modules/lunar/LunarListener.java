package dev.lorenzz.lxsmp.modules.lunar;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Lunar Client listener using Apollo API.
 * Handles Rich Presence updates on join/quit.
 */
public class LunarListener implements Listener {

    private final LXSMP plugin;
    private final LunarModule module;

    public LunarListener(LXSMP plugin, LunarModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Delay to let Apollo register the player
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Update Rich Presence
            module.updateRichPresence(uuid);

            // Send all existing teammate waypoints to the joining player
            module.sendAllTeammateWaypoints(uuid);

            // Send this player's waypoint to all other Lunar players
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(uuid)) continue;
                module.sendTeammateWaypoint(online.getUniqueId(), player);
            }
        }, 40L); // 2 second delay
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        module.resetRichPresence(uuid);
        module.removeAllTeammateWaypoints(uuid);

        // Remove this player's waypoint from all other Lunar players
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(uuid)) continue;
            module.removeTeammateWaypoint(online.getUniqueId(), player.getName());
        }
    }
}
