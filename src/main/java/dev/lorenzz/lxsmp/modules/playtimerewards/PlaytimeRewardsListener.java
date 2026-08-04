package dev.lorenzz.lxsmp.modules.playtimerewards;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.PlaytimeRewardsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Tracks player join/quit for active playtime tracking.
 */
public class PlaytimeRewardsListener implements Listener {

    private final LXSMP plugin;

    public PlaytimeRewardsListener(LXSMP plugin) {
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
