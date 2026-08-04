package dev.lorenzz.lxsmp.modules.spawn;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.SpawnManager;
import dev.lorenzz.lxsmp.utils.GUI;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CommandAlias("spawn")
public class SpawnCommands extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.25D;
    private static final Map<UUID, BukkitRunnable> pendingCountdowns = new ConcurrentHashMap<>();

    @Default
    @CommandPermission("solum.spawn")
    @Description("Teleport to spawn")
    public void spawn(Player player) {
        SpawnManager sm = SpawnManager.getInstance();
        if (sm == null) { Messages.send(player, "&cSpawn not available."); return; }

        Set<String> names = sm.getSpawnNames();
        if (names.isEmpty()) {
            Messages.send(player, plugin.getConfigFile().getString("spawn.messages.not-set", "&cNo spawn has been set!"));
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

    @CommandAlias("setspawn")
    @CommandPermission("solum.admin")
    @Description("Set a spawn point")
    public void setSpawn(Player player, @Optional String name) {
        if (name == null || name.isEmpty()) name = "default";
        SpawnManager sm = SpawnManager.getInstance();
        sm.setSpawn(name, player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("spawn.messages.set", "&7Spawn &b%name% &7has been set!")
                .replace("%name%", name));
    }

    private Sound getSound(String key, Sound fallback) {
        try {
            return Sound.valueOf(plugin.getConfigFile().getString("sounds." + key, fallback.name()));
        } catch (Exception e) {
            return fallback;
        }
    }

    private void teleportToSpawn(Player player, String name) {
        SpawnManager sm = SpawnManager.getInstance();
        Location loc = sm.getSpawn(name);
        if (loc == null) { Messages.send(player, "&cSpawn not found!"); return; }

        int delay = plugin.getConfigFile().getInt("spawn.teleport-delay-seconds", 3);

        if (player.hasPermission("solum.spawn.bypassdelay") || delay <= 0) {
            player.teleportAsync(loc).thenAccept(success -> {
                if (success) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.playSound(player.getLocation(), getSound("teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1.2f);
                        String successBar = plugin.getConfigFile().getString("spawn.messages.actionbar-success",
                                "&a✔ Teleported to spawn &b{name}").replace("{name}", name);
                        player.sendActionBar(Messages.colorComponent(successBar));
                        Messages.send(player, plugin.getConfigFile().getString("spawn.messages.teleported",
                                "&7Teleported to spawn &b%name%&7!").replace("%name%", name));
                    });
                }
            });
            return;
        }

        String teleportingMsg = plugin.getConfigFile().getString("spawn.messages.teleporting",
                        "&7Teleporting in &e{seconds} &7seconds... Don't move!")
                .replace("{seconds}", String.valueOf(delay));
        Messages.send(player, teleportingMsg);

        Location startLoc = player.getLocation().clone();
        UUID playerId = player.getUniqueId();

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

                if (hasMoved(startLoc, current.getLocation())) {
                    pendingCountdowns.remove(playerId);
                    cancel();
                    String movedMsg = plugin.getConfigFile().getString("spawn.messages.moved",
                            "&cYou moved! Teleport cancelled.");
                    Messages.send(current, movedMsg);
                    current.sendActionBar(Messages.colorComponent("&c✖ Teleport cancelled"));
                    return;
                }

                if (countdown > 0) {
                    current.playSound(current.getLocation(), getSound("teleport-countdown", Sound.BLOCK_NOTE_BLOCK_PLING), 1f, 1f);
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
                                current.playSound(current.getLocation(), getSound("teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1.2f);
                                String successBar = plugin.getConfigFile().getString("spawn.messages.actionbar-success",
                                        "&a✔ Teleported to spawn &b{name}").replace("{name}", name);
                                current.sendActionBar(Messages.colorComponent(successBar));
                                Messages.send(current, plugin.getConfigFile().getString("spawn.messages.teleported",
                                        "&7Teleported to spawn &b%name%&7!").replace("%name%", name));
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
