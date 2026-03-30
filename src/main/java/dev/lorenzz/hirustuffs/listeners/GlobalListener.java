package dev.lorenzz.hirustuffs.listeners;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AFKManager;
import dev.lorenzz.hirustuffs.managers.SpawnManager;
import dev.lorenzz.hirustuffs.managers.StatsManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;


public class GlobalListener implements Listener {

    private final Hiru plugin;
    // Throttle move events — only record activity every 2 seconds per player
    private final Map<UUID, Long> lastMoveRecord = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long MOVE_THROTTLE_MS = 2000L;

    public GlobalListener(Hiru plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Stats playtime
        if (StatsManager.getInstance() != null) {
            StatsManager.getInstance().startPlaytime(player.getUniqueId());
        }

        // AFK
        if (AFKManager.getInstance() != null) {
            AFKManager.getInstance().playerJoin(player.getUniqueId());
        }

        // Teleport to spawn only on first join; returning players keep their logout position
        if (SpawnManager.getInstance() != null && !player.hasPlayedBefore()) {
            Location spawnLoc = SpawnManager.getInstance().getDefaultSpawn();
            if (spawnLoc != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.teleportAsync(spawnLoc);
                    }
                }, 1L);
            }
        }
        player.sendMessage(Messages.color("""
                &r
                &r
                &r
                &#0053ff&lSMPUNI &7- &#d0cfd3S&#c9d7dae&#c3dfe1a&#bce6e9s&#b6eef0o&#aff6f7n ➀
                &r&7(/store - /crates - /duels - /afkzone - /changelog - /rtp - /billfords - /skilltree - /sell - /shop - /buy)
                &r
                &#d8e4e4Hello! 
                &#d8e4e4The first season of the SMPUNI is launched at &#89b4ca 29 March 2026! 
                &#d8e4e4After the beta testing phase, the world will be reset and all progress will be wiped.
                &#d8e4e4Thank you for being part of the testing phase, we fixxed any bugs!
                &#d8e4e4All players banned has been unbanned. for give another chance.
                
                &#89b4caWhat we addded? 
                &#d8e4e4- New Spawners shoppable with the Shards on the &#89b4ca/shop 
                &#d8e4e4- New VIP ranks with perks, purchasable with Shards on the &#89b4ca/buy
                &#d8e4e4- Duel System: Challenge other players to duels with custom kits and win Shards &#89b4ca/duels
                &#d8e4e4- Crates with random rewards, purchasable with Shards on the &#89b4ca/shop 
                &#d8e4e4- Afkzone with AFK rewards, earn Shards while being AFK in the &#89b4ca/afkzone
                &#d8e4e4- New AntiCheat System to ensure fair play and catch cheaters.
                
                &#d8e4e4- If you are intrested the changelogs of the server checkout our command &#89b4ca/changelog
                &r
                """));  // 2 second delay
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastMoveRecord.remove(player.getUniqueId());

        // Stats playtime
        if (StatsManager.getInstance() != null) {
            StatsManager.getInstance().stopPlaytime(player.getUniqueId());
        }

        // AFK
        if (AFKManager.getInstance() != null) {
            AFKManager.getInstance().playerQuit(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        StatsManager stats = StatsManager.getInstance();
        if (stats != null) {
            stats.addDeath(victim.getUniqueId());
            if (killer != null) {
                stats.addKill(killer.getUniqueId());
            }
        }

        // ── Kill Notifications ──
        if (killer != null && !killer.equals(victim)) {
            // Weapon name
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            String weaponName;
            if (weapon.getType() == Material.AIR) {
                weaponName = "Fists";
            } else if (weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName()) {
                weaponName = Messages.color(weapon.getItemMeta().getDisplayName());
            } else {
                weaponName = formatMaterialName(weapon.getType());
            }

            // Broadcast kill message
            Component killMessage = Messages.colorComponent(
                    "&#FF4444⚔ &#FF6B6B" + victim.getName() +
                    " &#8C8C8Cwas killed by &#55FF55" + killer.getName() +
                    " &#8C8C8Cusing &#FFD700" + weaponName);

            event.deathMessage(killMessage);
        } else {
            // Suicide / environmental death
            Component deathMessage = Messages.colorComponent(
                    "&#CE93D8☠ &#FF6B6B" + victim.getName() +
                    " &#8C8C8Ccommitted suicide");

            event.deathMessage(deathMessage);
        }
    }

    private String formatMaterialName(Material material) {
        String name = material.name().replace('_', ' ');
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(' ');
            }
        }
        return result.toString().trim();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        if (AFKManager.getInstance() != null) {
            UUID uuid = event.getPlayer().getUniqueId();
            long now = System.currentTimeMillis();
            Long last = lastMoveRecord.get(uuid);
            if (last == null || now - last >= MOVE_THROTTLE_MS) {
                lastMoveRecord.put(uuid, now);
                AFKManager.getInstance().recordActivity(uuid);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (AFKManager.getInstance() != null) {
            AFKManager.getInstance().recordActivity(event.getPlayer().getUniqueId());
        }
    }
}
