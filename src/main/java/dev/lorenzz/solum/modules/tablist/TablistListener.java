package dev.lorenzz.solum.modules.tablist;

import dev.lorenzz.solum.utils.JoinStagger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TablistListener implements Listener {

    private final TablistManager manager;
    private final JoinStagger stagger = new JoinStagger(10);

    public TablistListener(TablistManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(),
                () -> { if (player.isOnline()) manager.attach(player); },
                stagger.nextDelay());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.detach(event.getPlayer());
    }
}


