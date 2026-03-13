package dev.lorenzz.hirustuffs.modules.playtimerewards;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.PlaytimeRewardsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Tracks player join/quit for active playtime tracking.
 */
public class PlaytimeRewardsListener implements Listener {

    private final Hiru plugin;

    public PlaytimeRewardsListener(Hiru plugin) {
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
