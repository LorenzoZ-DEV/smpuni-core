package dev.lorenzz.solum.modules.combat;

import dev.lorenzz.solum.managers.TeamsManager;
import dev.lorenzz.solum.utils.Messages;
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

    // ── Player hits Player ──
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker.equals(victim)) return;

        // Check team friendly fire
        TeamsManager tm = TeamsManager.getInstance();
        if (tm != null) {
            TeamsManager.Team attackerTeam = tm.getPlayerTeam(attacker.getUniqueId());
            TeamsManager.Team victimTeam = tm.getPlayerTeam(victim.getUniqueId());
            if (attackerTeam != null && victimTeam != null
                    && attackerTeam.getName().equalsIgnoreCase(victimTeam.getName())) {
                if (!attackerTeam.isFriendlyFire()) {
                    // Friendly fire is off — don't tag
                    return;
                }
            }
        }

        // Bypass permission
        if (attacker.hasPermission("solum.combat.bypass") && victim.hasPermission("solum.combat.bypass")) {
            return;
        }

        cm.tagPlayer(attacker.getUniqueId(), victim.getUniqueId());
    }

    // ── Player death in combat — untag ──
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;

        Player victim = event.getEntity();
        if (cm.isInCombat(victim.getUniqueId())) {
            // Untag the attacker too
            java.util.UUID attackerUUID = cm.getLastAttacker(victim.getUniqueId());
            if (attackerUUID != null) {
                cm.untagPlayer(attackerUUID);
            }
            cm.untagPlayer(victim.getUniqueId());
        }
    }

    // ── Combat log detection ──
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;

        Player player = event.getPlayer();
        if (cm.isInCombat(player.getUniqueId())) {
            cm.handleCombatLogout(player);
        }
    }

    // ── Block commands in combat ──
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
            Messages.send(player, cm.getMessage("command-blocked",
                    "&#FF4444You cannot use that command while in combat!"));
        }
    }

    // ── Block ender pearl usage in combat ──
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;
        if (!cm.isEnderPearlBlocked()) return;

        if (event.getEntity() instanceof EnderPearl pearl) {
            if (pearl.getShooter() instanceof Player player) {
                if (cm.isInCombat(player.getUniqueId()) && !player.hasPermission("solum.combat.bypass")) {
                    event.setCancelled(true);
                    Messages.send(player, cm.getMessage("ender-pearl-blocked",
                            "&#FF4444You cannot use ender pearls while in combat!"));
                }
            }
        }
    }

    // ── Block chorus fruit in combat ──
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
            Messages.send(player, cm.getMessage("chorus-blocked",
                    "&#FF4444You cannot eat chorus fruit while in combat!"));
        }
    }

    // ── Block teleport in combat ──
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        CombatManager cm = CombatManager.getInstance();
        if (cm == null) return;
        if (!cm.isTeleportBlocked()) return;

        Player player = event.getPlayer();
        if (!cm.isInCombat(player.getUniqueId())) return;
        if (player.hasPermission("solum.combat.bypass")) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        // Allow respawns and internal plugin teleports caused by death
        if (cause == PlayerTeleportEvent.TeleportCause.SPECTATE) return;

        // Block ender pearl teleports, chorus, and command teleports
        if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                || cause == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
                || cause == PlayerTeleportEvent.TeleportCause.COMMAND) {
            event.setCancelled(true);
            Messages.send(player, cm.getMessage("teleport-blocked",
                    "&#FF4444You cannot teleport while in combat!"));
        }
    }

    // ── Block flight toggle in combat ──
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
            Messages.send(player, cm.getMessage("fly-disabled",
                    "&#FF4444Flight disabled — you are in combat!"));
        }
    }

    // ── Utility: resolve attacker from event (direct hit or projectile) ──
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
