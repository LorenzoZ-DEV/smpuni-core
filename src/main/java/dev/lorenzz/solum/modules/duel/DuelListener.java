package dev.lorenzz.solum.modules.duel;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.DuelManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class DuelListener implements Listener {

    private final Solum plugin;

    public DuelListener(Solum plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        DuelManager dm = DuelManager.getInstance();
        if (dm == null || !dm.isInDuel(dead.getUniqueId())) return;

        DuelManager.DuelState state = dm.getDuelState(dead.getUniqueId());
        if (state == null) return;

        // Cancel drops and death message in duel
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.deathMessage(null);

        // End duel — opponent wins
        dm.endDuel(state.getOpponent(), dead.getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DuelManager dm = DuelManager.getInstance();
        if (dm == null || !dm.isInDuel(player.getUniqueId())) return;

        DuelManager.DuelState state = dm.getDuelState(player.getUniqueId());
        if (state == null) return;

        // Player disconnected — opponent wins
        dm.endDuel(state.getOpponent(), player.getUniqueId(), true);

        String disconnectMsg = plugin.getConfigFile().getString("duel.messages.disconnect",
                        "&c⚠ &b%player% &cdisconnected during a duel!")
                .replace("%player%", player.getName());
        plugin.getServer().broadcast(Messages.colorComponent(Messages.getPrefix() + disconnectMsg));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        DuelManager dm = DuelManager.getInstance();
        if (dm == null) return;

        // If one is in duel but not with the other, cancel damage
        if (dm.isInDuel(victim.getUniqueId()) || dm.isInDuel(attacker.getUniqueId())) {
            DuelManager.DuelState victimState = dm.getDuelState(victim.getUniqueId());
            DuelManager.DuelState attackerState = dm.getDuelState(attacker.getUniqueId());

            // Allow damage only between duel opponents
            if (victimState != null && attackerState != null
                    && victimState.getOpponent().equals(attacker.getUniqueId())
                    && attackerState.getOpponent().equals(victim.getUniqueId())) {

                // Apply overtime damage multiplier
                double multiplier = dm.getDamageMultiplier(victim.getUniqueId());
                if (multiplier > 1.0) {
                    event.setDamage(event.getDamage() * multiplier);
                }
                return; // Allow
            }

            // One or both are in duel but not with each other
            if (dm.isInDuel(victim.getUniqueId()) || dm.isInDuel(attacker.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        DuelManager dm = DuelManager.getInstance();
        if (dm == null || !dm.isInDuel(player.getUniqueId())) return;

        String cmd = event.getMessage().toLowerCase();
        // Allow duel commands
        if (cmd.startsWith("/duel") || cmd.startsWith("/msg") || cmd.startsWith("/r ") || cmd.startsWith("/reply")) {
            return;
        }

        boolean blockCommands = plugin.getConfigFile().getBoolean("duel.block-commands", true);
        if (blockCommands) {
            event.setCancelled(true);
            Messages.send(player, plugin.getConfigFile().getString("duel.messages.command-blocked",
                    "&cYou cannot use commands during a duel!"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        DuelManager dm = DuelManager.getInstance();
        if (dm == null || !dm.isInDuel(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (dm.isKitBook(item)) {
            event.setCancelled(true);
            dm.giveDuelKit(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        DuelManager dm = DuelManager.getInstance();
        if (dm == null || !dm.isInDuel(player.getUniqueId())) return;

        // Prevent dropping the kit book
        if (dm.isKitBook(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }
}
