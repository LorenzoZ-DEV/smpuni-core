package dev.lorenzz.solum.modules.scoreboard;

import dev.lorenzz.solum.utils.JoinStagger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ScoreboardListener implements Listener {

    private final ScoreboardManager manager;
    private final JoinStagger stagger = new JoinStagger(10);

    public ScoreboardListener(ScoreboardManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                () -> { if (player.isOnline()) manager.createBoard(player); },
                stagger.nextDelay());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.removeBoard(event.getPlayer());
    }
}



