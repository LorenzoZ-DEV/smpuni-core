package dev.lorenzz.solum.modules.playtimerewards;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.PlaytimeRewardsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Tracks player join/quit for active playtime tracking.
 */
public class PlaytimeRewardsListener implements Listener {

    private final Solum plugin;

    public PlaytimeRewardsListener(Solum plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlaytimeRewardsManager prm = PlaytimeRewardsManager.getInstance();
        if (prm != null) {
            prm.playerJoin(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlaytimeRewardsManager prm = PlaytimeRewardsManager.getInstance();
        if (prm != null) {
            prm.playerQuit(event.getPlayer().getUniqueId());
        }
    }
}
