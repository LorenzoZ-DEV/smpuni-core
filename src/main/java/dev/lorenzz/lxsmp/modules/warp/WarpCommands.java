package dev.lorenzz.lxsmp.modules.warp;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.WarpManager;
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

public class WarpCommands extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.25D;
    private static final Map<UUID, BukkitRunnable> pendingCountdowns = new ConcurrentHashMap<>();

    private Sound getSound(String key, Sound fallback) {
        try {
            return Sound.valueOf(plugin.getConfigFile().getString("sounds." + key, fallback.name()));
        } catch (Exception e) {
            return fallback;
        }
    }

    @CommandAlias("warp")
    @CommandPermission("solum.warp")
    @Description("Teleport to a warp")
    public void warp(Player player, @Optional @Single String name) {
        WarpManager wm = WarpManager.getInstance();
        if (wm == null) { Messages.send(player, "&cWarps not available."); return; }

        if (name == null || name.isEmpty()) {
            openWarpGUI(player);
            return;
        }

        Location loc = wm.getWarp(name);
        if (loc == null) {
            Messages.send(player, plugin.getConfigFile().getString("warp.messages.not-found", "&cWarp &e%name% &cnot found!")
                    .replace("%name%", name));
            return;
        }
        teleportToWarp(player, name, loc);
    }

    @CommandAlias("setwarp")
    @CommandPermission("solum.admin")
    @Description("Set a warp point")
    public void setWarp(Player player, @Single String name) {
        WarpManager wm = WarpManager.getInstance();
        if (wm == null) { Messages.send(player, "&cWarps not available."); return; }

        wm.setWarp(name, player.getLocation());
        Messages.send(player, plugin.getConfigFile().getString("warp.messages.set", "&7Warp &b%name% &7has been set!")
                .replace("%name%", name));
    }

    @CommandAlias("delwarp")
    @CommandPermission("solum.admin")
    @Description("Delete a warp point")
    public void delWarp(Player player, @Single String name) {
        WarpManager wm = WarpManager.getInstance();
        if (wm == null) { Messages.send(player, "&cWarps not available."); return; }

        if (wm.deleteWarp(name)) {
            Messages.send(player, plugin.getConfigFile().getString("warp.messages.deleted", "&7Warp &b%name% &7has been deleted!")
                    .replace("%name%", name));
        } else {
            Messages.send(player, plugin.getConfigFile().getString("warp.messages.not-found", "&cWarp &e%name% &cnot found!")
                    .replace("%name%", name));
        }
    }

    @CommandAlias("warps")
    @CommandPermission("solum.warp")
    @Description("List all warps")
    public void warps(Player player) {
        openWarpGUI(player);
    }

    private void openWarpGUI(Player player) {
        WarpManager wm = WarpManager.getInstance();
        Set<String> names = wm.getWarpNames();
        if (names.isEmpty()) {
            Messages.send(player, plugin.getConfigFile().getString("warp.messages.no-warps", "&cNo warps available!"));
            return;
        }

        String title = plugin.getConfigFile().getString("warp.gui-title", "&8Warps");
        int rows = Math.min((names.size() / 7) + 2, 6);
        GUI.Builder builder = GUI.builder(title, rows).border(Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (String name : names) {
            builder.item(slot, Material.ENDER_PEARL, "&b" + name,
                    List.of("&7Click to teleport to &b" + name), p -> {
                        Location loc = wm.getWarp(name);
                        if (loc != null) teleportToWarp(p, name, loc);
                    });
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        builder.open(player);
    }

    private void teleportToWarp(Player player, String name, Location loc) {
        int delay = plugin.getConfigFile().getInt("warp.teleport-delay-seconds", 3);

        if (player.hasPermission("solum.warp.bypassdelay") || delay <= 0) {
            player.teleportAsync(loc).thenAccept(success -> {
                if (success) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.playSound(player.getLocation(), getSound("teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1.2f);
                        String successBar = plugin.getConfigFile().getString("warp.messages.actionbar-success",
                                "&a✔ Teleported to warp &b{name}").replace("{name}", name);
                        player.sendActionBar(Messages.colorComponent(successBar));
                        Messages.send(player, plugin.getConfigFile().getString("warp.messages.teleported",
                                "&7Teleported to warp &b%name%&7!").replace("%name%", name));
                    });
                }
            });
            return;
        }

        String teleportingMsg = plugin.getConfigFile().getString("warp.messages.teleporting",
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
                    String movedMsg = plugin.getConfigFile().getString("warp.messages.moved",
                            "&cYou moved! Teleport cancelled.");
                    Messages.send(current, movedMsg);
                    current.sendActionBar(Messages.colorComponent("&c✖ Teleport cancelled"));
                    return;
                }

                if (countdown > 0) {
                    current.playSound(current.getLocation(), getSound("teleport-countdown", Sound.BLOCK_NOTE_BLOCK_PLING), 1f, 1f);
                    String actionBar = plugin.getConfigFile().getString("warp.messages.actionbar-countdown",
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
                                String successBar = plugin.getConfigFile().getString("warp.messages.actionbar-success",
                                        "&a✔ Teleported to warp &b{name}").replace("{name}", name);
                                current.sendActionBar(Messages.colorComponent(successBar));
                                Messages.send(current, plugin.getConfigFile().getString("warp.messages.teleported",
                                        "&7Teleported to warp &b%name%&7!").replace("%name%", name));
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
