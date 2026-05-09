package dev.lorenzz.solum.modules.nametag;

import dev.lorenzz.solum.Solum;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class NametagListener implements Listener {

    private final NametagManager manager;

    public NametagListener(NametagManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        manager.attach(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.detach(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDeath(PlayerDeathEvent event) {
        manager.detach(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        manager.detach(event.getPlayer());
        manager.attach(event.getPlayer());
    }

    /** Refresh on damage so the heart/health value updates instantly (UnlimitedNametags style). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        // Health is decremented AFTER the event → schedule next tick.
        Bukkit.getScheduler().runTask(Solum.getInstance(), () -> manager.refresh(p));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        Bukkit.getScheduler().runTask(Solum.getInstance(), () -> manager.refresh(p));
    }

    /** Re-apply anchor when the player's bounding box changes (height-based offset). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Bukkit.getScheduler().runTask(Solum.getInstance(),
                () -> manager.refresh(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwim(EntityToggleSwimEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        Bukkit.getScheduler().runTask(Solum.getInstance(), () -> manager.refresh(p));
    }
}
