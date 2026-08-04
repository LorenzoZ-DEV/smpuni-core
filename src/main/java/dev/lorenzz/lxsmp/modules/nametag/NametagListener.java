package dev.lorenzz.lxsmp.modules.nametag;

import dev.lorenzz.lxsmp.utils.JoinStagger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NametagListener implements Listener {

    private final NametagManager manager;
    private final JoinStagger stagger = new JoinStagger(10);

    public NametagListener(NametagManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(manager.getPlugin(), () -> {
            if (player.isOnline() && !player.isDead()) manager.attach(player);
        }, stagger.nextDelay());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.detach(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        manager.detach(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
            if (player.isOnline() && !player.isDead()) {
                manager.detach(player);
                manager.attach(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
            Player p = event.getPlayer();
            if (p.isOnline() && !p.isDead()) {
                manager.detach(p);
                manager.attach(p);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleportPre(PlayerTeleportEvent event) {
        manager.dismountPassenger(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(manager.getPlugin(), () -> {
            if (!player.isOnline() || player.isDead()) return;
            manager.detach(player);
            manager.attach(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;
        manager.followPlayer(event.getPlayer());
    }
}

