package dev.lorenzz.hirustuffs.modules.back;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackCommand {

    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.25D;
    private static final int WARMUP_SECONDS = 3;

    private final Map<UUID, Location> pendingOrigins = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> pendingCountdowns = new ConcurrentHashMap<>();

    @Command(name = "back", description = "Teleport to your death location", async = false)
    @Permission("hiru.back")
    public void back(@Sender Player player) {
        BackModule module = BackModule.getInstance();
        if (module == null || module.getBackManager() == null) {
            Messages.send(player, "&#FF4444Back system is not available.");
            return;
        }

        BackManager manager = module.getBackManager();

        if (!manager.hasDeathLocation(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You have no death location to return to.");
            return;
        }

        // Cooldown check
        if (!player.hasPermission("hiru.back.bypasscooldown")) {
            long remaining = manager.getRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                Messages.send(player, "&#FF4444You must wait &#FFD700" + remaining + "s &#FF4444before using /back again.");
                return;
            }
        }

        // Bypass warmup for admins
        if (player.hasPermission("hiru.back.bypasswarmup")) {
            executeTeleport(player, manager);
            return;
        }

        // Already pending?
        if (pendingCountdowns.containsKey(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You are already being teleported...");
            return;
        }

        // Start warmup countdown
        pendingOrigins.put(player.getUniqueId(), player.getLocation().clone());
        Messages.send(player, "&#E0E0E0Teleporting in &#FFD700" + WARMUP_SECONDS + " seconds&#E0E0E0... Don't move!");

        UUID playerId = player.getUniqueId();

        BukkitRunnable countdown = new BukkitRunnable() {
            int remaining = WARMUP_SECONDS;

            @Override
            public void run() {
                Player current = Bukkit.getPlayer(playerId);
                if (current == null || !current.isOnline()) {
                    cleanup(playerId);
                    cancel();
                    return;
                }

                // Check movement
                Location origin = pendingOrigins.get(playerId);
                if (origin == null || hasMoved(origin, current.getLocation())) {
                    Messages.send(current, "&#FF4444Teleportation cancelled — you moved!");
                    cleanup(playerId);
                    cancel();
                    return;
                }

                if (remaining > 0) {
                    current.playSound(current.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    current.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(Messages.color(
                                    "&#FFD700⏳ Teleporting in &#FFFFFF" + remaining + "s&#FFD700...")));
                    remaining--;
                } else {
                    cleanup(playerId);
                    cancel();
                    executeTeleport(current, manager);
                }
            }
        };

        pendingCountdowns.put(playerId, countdown);
        countdown.runTaskTimer(dev.lorenzz.hirustuffs.Hiru.getInstance(), 0L, 20L);
    }

    private void executeTeleport(Player player, BackManager manager) {
        Location deathLoc = manager.consumeDeathLocation(player.getUniqueId());
        if (deathLoc == null || deathLoc.getWorld() == null) {
            Messages.send(player, "&#FF4444Your death location is no longer available.");
            return;
        }

        player.teleportAsync(deathLoc).thenAccept(success -> {
            if (success) {
                manager.startCooldown(player.getUniqueId());
                Messages.send(player, "&#55FF55✔ Teleported to your death location!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(Messages.color(
                                "&#55FF55✔ Arrived at death location")));
            } else {
                Messages.send(player, "&#FF4444Failed to teleport to your death location.");
            }
        });
    }

    private void cleanup(UUID playerId) {
        pendingOrigins.remove(playerId);
        pendingCountdowns.remove(playerId);
    }

    private boolean hasMoved(Location origin, Location current) {
        if (origin == null || current == null) return true;
        if (origin.getWorld() == null || current.getWorld() == null) return true;
        if (!origin.getWorld().equals(current.getWorld())) return true;
        return origin.distanceSquared(current) > MOVEMENT_TOLERANCE_SQUARED;
    }
}
