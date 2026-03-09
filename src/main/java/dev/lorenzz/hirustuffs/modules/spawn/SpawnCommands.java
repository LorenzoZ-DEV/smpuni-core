package dev.lorenzz.hirustuffs.modules.spawn;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Named;
import dev.lorenzz.common.annotations.Optional;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.SpawnManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnCommands {

    private final Hiru plugin = Hiru.getInstance();

    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.25D;
    private static final Map<UUID, BukkitRunnable> pendingCountdowns = new ConcurrentHashMap<>();

    @Command(name = "spawn", description = "Teletrasportati allo spawn", async = false)
    @Permission("hiru.spawn")
    public void spawn(@Sender Player player) {
        SpawnManager sm = SpawnManager.getInstance();
        if (sm == null) { Messages.send(player, "&cSpawn non disponibile."); return; }

        Set<String> names = sm.getSpawnNames();
        if (names.isEmpty()) {
            Messages.send(player, plugin.getConfigFile().getString("spawn.messages.not-set", "&cNessuno spawn impostato!"));
            return;
        }

        if (names.size() == 1) {
            teleportToSpawn(player, names.iterator().next());
            return;
        }

        String title = plugin.getConfigFile().getString("spawn.messages.menu-title", "&8Spawn Menu");
        int rows = Math.min((names.size() / 9) + 2, 6);
        GUI.Builder builder = GUI.builder(title, rows).border(Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (String name : names) {
            builder.item(slot, Material.ENDER_PEARL, "&b" + name,
                    List.of("&7Click to teleport"), p -> teleportToSpawn(p, name));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        builder.open(player);
    }

    @Command(name = "setspawn", description = "Imposta uno spawn", async = false)
    @Permission("hiru.admin")
    public void setSpawn(@Sender Player player, @Optional("default") @Named("nome") String name) {
        SpawnManager sm = SpawnManager.getInstance();
        sm.setSpawn(name, player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("spawn.messages.set", "&7Spawn %name% impostato!")
                .replace("%name%", name));
    }

    private void teleportToSpawn(Player player, String name) {
        SpawnManager sm = SpawnManager.getInstance();
        Location loc = sm.getSpawn(name);
        if (loc == null) { Messages.send(player, "&cSpawn non trovato!"); return; }

        int delay = plugin.getConfigFile().getInt("spawn.teleport-delay-seconds", 3);

        // Bypass delay with permission
        if (player.hasPermission("hiru.spawn.bypassdelay") || delay <= 0) {
            player.teleportAsync(loc).thenAccept(success -> {
                if (success) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        String successBar = plugin.getConfigFile().getString("spawn.messages.actionbar-success",
                                "&a✔ Teleported to spawn &b{name}").replace("{name}", name);
                        player.sendActionBar(Messages.colorComponent(successBar));
                        Messages.send(player, plugin.getConfigFile().getString("spawn.messages.teleported",
                                "&7Teletrasportato allo spawn &b%name%&7!").replace("%name%", name));
                    });
                }
            });
            return;
        }

        String teleportingMsg = plugin.getConfigFile().getString("spawn.messages.teleporting",
                        "&7Teletrasporto tra &e{seconds} &7secondi... Non muoverti!")
                .replace("{seconds}", String.valueOf(delay));
        Messages.send(player, teleportingMsg);

        Location startLoc = player.getLocation().clone();
        UUID playerId = player.getUniqueId();

        // Cancel any existing countdown
        BukkitRunnable existing = pendingCountdowns.remove(playerId);
        if (existing != null) existing.cancel();

        BukkitRunnable countdownTask = new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                Player current = Bukkit.getPlayer(playerId);
                if (current == null || !current.isOnline()) {
                    pendingCountdowns.remove(playerId);
                    cancel();
                    return;
                }

                // Check movement
                if (hasMoved(startLoc, current.getLocation())) {
                    pendingCountdowns.remove(playerId);
                    cancel();
                    String movedMsg = plugin.getConfigFile().getString("spawn.messages.moved",
                            "&cTi sei mosso! Teletrasporto annullato.");
                    Messages.send(current, movedMsg);
                    current.sendActionBar(Messages.colorComponent("&c✖ Teleport cancelled"));
                    return;
                }

                if (countdown > 0) {
                    current.playSound(current.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    String actionBar = plugin.getConfigFile().getString("spawn.messages.actionbar-countdown",
                            "&e⏳ Teleporting in &c{seconds}s&e...").replace("{seconds}", String.valueOf(countdown));
                    current.sendActionBar(Messages.colorComponent(actionBar));
                    countdown--;
                } else {
                    pendingCountdowns.remove(playerId);
                    cancel();

                    current.teleportAsync(loc).thenAccept(success -> {
                        if (success) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                current.playSound(current.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                                String successBar = plugin.getConfigFile().getString("spawn.messages.actionbar-success",
                                        "&a✔ Teleported to spawn &b{name}").replace("{name}", name);
                                current.sendActionBar(Messages.colorComponent(successBar));
                                Messages.send(current, plugin.getConfigFile().getString("spawn.messages.teleported",
                                        "&7Teletrasportato allo spawn &b%name%&7!").replace("%name%", name));
                            });
                        }
                    });
                }
            }
        };

        pendingCountdowns.put(playerId, countdownTask);
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    private boolean hasMoved(Location origin, Location current) {
        if (origin == null || current == null) return true;
        if (origin.getWorld() == null || current.getWorld() == null) return true;
        if (!Objects.equals(origin.getWorld(), current.getWorld())) return true;
        return origin.distanceSquared(current) > MOVEMENT_TOLERANCE_SQUARED;
    }
}
