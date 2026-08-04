package dev.lorenzz.lxsmp.modules.combat;

import dev.lorenzz.lxsmp.managers.TeamsManager;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class CombatListener implements Listener {

    public CombatListener() {
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker.equals(victim)) return;

        TeamsManager tm = TeamsManager.getInstance();
        if (tm != null) {
            TeamsManager.Team attackerTeam = tm.getPlayerTeam(attacker.getUniqueId());
            TeamsManager.Team victimTeam = tm.getPlayerTeam(victim.getUniqueId());
            if (attackerTeam != null && victimTeam != null
                    && attackerTeam.getName().equalsIgnoreCase(victimTeam.getName())) {
                if (!attackerTeam.isFriendlyFire()) {
                    return;
                }
            }
        }

        if (attacker.hasPermission("solum.combat.bypass") && victim.hasPermission("solum.combat.bypass")) {
            return;
        }

        cm.tagPlayer(attacker.getUniqueId(), victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;

        Player victim = event.getEntity();
        if (cm.isInCombat(victim.getUniqueId())) {
            java.util.UUID attackerUUID = cm.getLastAttacker(victim.getUniqueId());
            if (attackerUUID != null) {
                cm.untagPlayer(attackerUUID);
            }
            cm.untagPlayer(victim.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;

        Player player = event.getPlayer();
        if (cm.isInCombat(player.getUniqueId())) {
            cm.handleCombatLogout(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;

        Player player = event.getPlayer();
        if (!cm.isInCombat(player.getUniqueId())) return;
        if (player.hasPermission("solum.combat.bypass")) return;

        String command = event.getMessage();
        if (cm.isCommandBlocked(command)) {
            event.setCancelled(true);
            Messages.send(player, cm.getMessage("command-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;
        if (!cm.isEnderPearlBlocked()) return;

        if (event.getEntity() instanceof EnderPearl pearl) {
            if (pearl.getShooter() instanceof Player player) {
                if (cm.isInCombat(player.getUniqueId()) && !player.hasPermission("solum.combat.bypass")) {
                    event.setCancelled(true);
                    Messages.send(player, cm.getMessage("ender-pearl-blocked"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;
        if (!cm.isChorusFruitBlocked()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item.getType() == Material.CHORUS_FRUIT
                && cm.isInCombat(player.getUniqueId())
                && !player.hasPermission("solum.combat.bypass")) {
            event.setCancelled(true);
            Messages.send(player, cm.getMessage("chorus-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;
        if (!cm.isTeleportBlocked()) return;

        Player player = event.getPlayer();
        if (!cm.isInCombat(player.getUniqueId())) return;
        if (player.hasPermission("solum.combat.bypass")) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.SPECTATE) return;

        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
                || cause == PlayerTeleportEvent.TeleportCause.COMMAND) {
            event.setCancelled(true);
            Messages.send(player, cm.getMessage("teleport-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;
        if (!cm.isFlyingBlocked()) return;

        Player player = event.getPlayer();
        if (cm.isInCombat(player.getUniqueId()) && !player.hasPermission("solum.combat.bypass")) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setFlying(false);
            Messages.send(player, cm.getMessage("fly-disabled"));
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) {
            return p;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player p) {
                return p;
            }
        }
        return null;
    }
}
