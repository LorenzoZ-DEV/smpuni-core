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


public class GlobalListener implements Listener {

    private final Hiru plugin;

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
                &#0053ff&lSMPUNI &7- &
                &fᴡᴇʟᴄᴏᴍᴇ &fᴛᴏ &#0053ffѕᴍᴘᴜɴɪ
                &fᴛʜɪѕ ɢᴀᴍᴇᴍᴏᴅᴇ ɪѕ &#0053ff ᴅᴇᴠᴇʟᴏᴘᴇᴅ &fᴡɪᴛʜᴏᴜᴛ &#0053ffᴠɪᴘ ʀᴀɴᴋѕ
                &fᴏᴜʀ ɡᴏᴀʟ ɪѕ ɴᴏᴛ ᴛᴏ ᴍᴀᴋᴇ ᴛʜᴇ ѕᴇʀᴠᴇʀ &#0053ffᴘᴀʏ ᴛᴏ ᴡɪɴ  
                &r
                &fThe beta will be ending at &c&l26/03/2026
                """));  // 2 second delay
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

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
            AFKManager.getInstance().recordActivity(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (AFKManager.getInstance() != null) {
            AFKManager.getInstance().recordActivity(event.getPlayer().getUniqueId());
        }
    }
}
