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
                &#0053ff&lSMPUNI &7- &e&lSEASON 1
                &r
                &fWelcome to &bSMPUni&f, a unique SMP experience with custom features and a friendly community! Join our Discord for support, announcements, and to connect with other players: &bhttps://discord.gg/smpuni
                &fThis server is supported also in 26.1 version, but we recommend using 1.21+ for the best experience.
                &fThe season 1 map is now live! Explore the world, gather resources, and build your base. Use &b/warp &fto find warps around the world, and &b/spawn &fto return to the main spawn area.
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
