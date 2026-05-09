package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TPAManager {

    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.25D;

    private static TPAManager INSTANCE;

    private final Solum plugin;
    private final Map<UUID, UUID> requests = new ConcurrentHashMap<>(); // target -> requester
    private final Map<UUID, UUID> hereRequests = new ConcurrentHashMap<>(); // target -> requester (tpahere)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> pendingCountdowns = new ConcurrentHashMap<>();

    public TPAManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    private Sound getSound(String key, Sound fallback) {
        try {
            return Sound.valueOf(plugin.getConfigFile().getString("sounds." + key, fallback.name()));
        } catch (Exception e) {
            return fallback;
        }
    }

    public static TPAManager getInstance() {
        return INSTANCE;
    }

    public boolean sendRequest(Player sender, Player target) {
        int cooldown = plugin.getConfigFile().getInt("tpa.cooldown-seconds", 30);
        int timeout = plugin.getConfigFile().getInt("tpa.timeout-seconds", 60);

        if (!sender.hasPermission("solum.tpa.bypasscooldown") && cooldowns.containsKey(sender.getUniqueId())) {
            long remaining = (cooldowns.get(sender.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                String msg = plugin.getConfigFile().getString("tpa.messages.cooldown", "&cWait %seconds%s.")
                        .replace("%seconds%", String.valueOf(remaining));
                Messages.send(sender, msg);
                return false;
            }
        }

        requests.put(target.getUniqueId(), sender.getUniqueId());
        cooldowns.put(sender.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));

        // Expiry task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (requests.remove(target.getUniqueId(), sender.getUniqueId())) {
                String expiredMsg = plugin.getConfigFile().getString("tpa.messages.expired", "&7The TPA request has expired.");
                if (sender.isOnline()) Messages.send(sender, expiredMsg);
                if (target.isOnline()) Messages.send(target, expiredMsg);
            }
            expiryTasks.remove(target.getUniqueId());
        }, timeout * 20L);
        expiryTasks.put(target.getUniqueId(), task);

        return true;
    }

    public boolean sendHereRequest(Player sender, Player target) {
        int cooldown = plugin.getConfigFile().getInt("tpa.cooldown-seconds", 30);
        int timeout = plugin.getConfigFile().getInt("tpa.timeout-seconds", 60);

        if (!sender.hasPermission("solum.tpa.bypasscooldown") && cooldowns.containsKey(sender.getUniqueId())) {
            long remaining = (cooldowns.get(sender.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                String msg = plugin.getConfigFile().getString("tpa.messages.cooldown", "&cWait %seconds%s.")
                        .replace("%seconds%", String.valueOf(remaining));
                Messages.send(sender, msg);
                return false;
            }
        }

        hereRequests.put(target.getUniqueId(), sender.getUniqueId());
        cooldowns.put(sender.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (hereRequests.remove(target.getUniqueId(), sender.getUniqueId())) {
                String expiredMsg = plugin.getConfigFile().getString("tpa.messages.expired", "&7The TPA request has expired.");
                if (sender.isOnline()) Messages.send(sender, expiredMsg);
                if (target.isOnline()) Messages.send(target, expiredMsg);
            }
            expiryTasks.remove(target.getUniqueId());
        }, timeout * 20L);
        expiryTasks.put(target.getUniqueId(), task);

        return true;
    }

    public boolean acceptRequest(Player target) {
        // Check for tpahere request first
        UUID hereRequesterId = hereRequests.remove(target.getUniqueId());
        if (hereRequesterId != null) {
            cancelExpiry(target.getUniqueId());
            return acceptHereRequest(target, hereRequesterId);
        }

        UUID requesterId = requests.remove(target.getUniqueId());
        if (requesterId == null) return false;

        cancelExpiry(target.getUniqueId());

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) return false;

        int delay = plugin.getConfigFile().getInt("tpa.teleport-delay-seconds", 3);

        // Bypass delay with permission
        if (requester.hasPermission("solum.tpa.bypassdelay") || delay <= 0) {
            requester.teleportAsync(target.getLocation()).thenAccept(success -> {
                if (success) {
                    requester.playSound(requester.getLocation(), getSound("teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1.2f);
                    String msg = plugin.getConfigFile().getString("tpa.messages.teleported",
                            "&7Teleported to &b%player%&7!").replace("%player%", target.getName());
                    Messages.send(requester, msg);
                }
            });
            return true;
        }

        String teleportingMsg = plugin.getConfigFile().getString("tpa.messages.teleporting",
                        "&7Teleporting in &e%seconds% &7seconds... Don't move!")
                .replace("%seconds%", String.valueOf(delay));
        Messages.send(requester, teleportingMsg);

        Location startLoc = requester.getLocation().clone();
        UUID requesterUUID = requester.getUniqueId();

        // Cancel any existing countdown for this player
        BukkitRunnable existing = pendingCountdowns.remove(requesterUUID);
        if (existing != null) existing.cancel();

        BukkitRunnable countdownTask = new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                Player current = Bukkit.getPlayer(requesterUUID);
                if (current == null || !current.isOnline()) {
                    pendingCountdowns.remove(requesterUUID);
                    cancel();
                    return;
                }

                // Check if moved
                if (hasMoved(startLoc, current.getLocation())) {
                    pendingCountdowns.remove(requesterUUID);
                    cancel();
                    String movedMsg = plugin.getConfigFile().getString("tpa.messages.moved", "&cYou moved! Teleport cancelled.");
                    Messages.send(current, movedMsg);
                    current.sendActionBar(Messages.colorComponent("&c✖ Teleport cancelled"));
                    return;
                }

                if (countdown > 0) {
                    current.playSound(current.getLocation(), getSound("teleport-countdown", Sound.BLOCK_NOTE_BLOCK_PLING), 1f, 1f);
                    String actionBar = plugin.getConfigFile().getString("tpa.messages.actionbar-countdown",
                            "&e⏳ Teleporting in &c{seconds}s&e...").replace("{seconds}", String.valueOf(countdown));
                    current.sendActionBar(Messages.colorComponent(actionBar));
                    countdown--;
                } else {
                    pendingCountdowns.remove(requesterUUID);
                    cancel();

                    Player tgt = Bukkit.getPlayer(target.getUniqueId());
                    Location teleportLoc = (tgt != null && tgt.isOnline()) ? tgt.getLocation() : target.getLocation();

                    current.teleportAsync(teleportLoc).thenAccept(success -> {
                        if (success) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                current.playSound(current.getLocation(), getSound("teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1.2f);
                                String successBar = plugin.getConfigFile().getString("tpa.messages.actionbar-success",
                                        "&a✔ Teleported to &b{player}").replace("{player}", target.getName());
                                current.sendActionBar(Messages.colorComponent(successBar));
                                String msg = plugin.getConfigFile().getString("tpa.messages.teleported",
                                        "&7Teleported to &b%player%&7!").replace("%player%", target.getName());
                                Messages.send(current, msg);
                            });
                        }
                    });
                }
            }
        };

        pendingCountdowns.put(requesterUUID, countdownTask);
        countdownTask.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    /**
     * Handles acceptance of a TPAHere request — the target teleports to the requester.
     */
    private boolean acceptHereRequest(Player target, UUID requesterId) {
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) return false;

        int delay = plugin.getConfigFile().getInt("tpa.teleport-delay-seconds", 3);

        if (target.hasPermission("solum.tpa.bypassdelay") || delay <= 0) {
            target.teleportAsync(requester.getLocation()).thenAccept(success -> {
                if (success) {
                    target.playSound(target.getLocation(), getSound("teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1.2f);
                    String msg = plugin.getConfigFile().getString("tpa.messages.teleported",
                            "&7Teleported to &b%player%&7!").replace("%player%", requester.getName());
                    Messages.send(target, msg);
                    Messages.send(requester, plugin.getConfigFile().getString("tpa.messages.tpahere-accepted",
                            "&b%player% &7teleported to you!").replace("%player%", target.getName()));
                }
            });
            return true;
        }

        String teleportingMsg = plugin.getConfigFile().getString("tpa.messages.teleporting",
                        "&7Teleporting in &e%seconds% &7seconds... Don't move!")
                .replace("%seconds%", String.valueOf(delay));
        Messages.send(target, teleportingMsg);

        Location startLoc = target.getLocation().clone();
        UUID targetUUID = target.getUniqueId();

        BukkitRunnable existing = pendingCountdowns.remove(targetUUID);
        if (existing != null) existing.cancel();

        BukkitRunnable countdownTask = new BukkitRunnable() {
            int countdown = delay;

            @Override
            public void run() {
                Player current = Bukkit.getPlayer(targetUUID);
                if (current == null || !current.isOnline()) {
                    pendingCountdowns.remove(targetUUID);
                    cancel();
                    return;
                }

                if (hasMoved(startLoc, current.getLocation())) {
                    pendingCountdowns.remove(targetUUID);
                    cancel();
                    String movedMsg = plugin.getConfigFile().getString("tpa.messages.moved", "&cYou moved! Teleport cancelled.");
                    Messages.send(current, movedMsg);
                    current.sendActionBar(Messages.colorComponent("&c✖ Teleport cancelled"));
                    return;
                }

                if (countdown > 0) {
                    current.playSound(current.getLocation(), getSound("teleport-countdown", Sound.BLOCK_NOTE_BLOCK_PLING), 1f, 1f);
                    String actionBar = plugin.getConfigFile().getString("tpa.messages.actionbar-countdown",
                            "&e⏳ Teleporting in &c{seconds}s&e...").replace("{seconds}", String.valueOf(countdown));
                    current.sendActionBar(Messages.colorComponent(actionBar));
                    countdown--;
                } else {
                    pendingCountdowns.remove(targetUUID);
                    cancel();

                    Player req = Bukkit.getPlayer(requesterId);
                    Location teleportLoc = (req != null && req.isOnline()) ? req.getLocation() : requester.getLocation();

                    current.teleportAsync(teleportLoc).thenAccept(success -> {
                        if (success) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                current.playSound(current.getLocation(), getSound("teleport", Sound.ENTITY_ENDERMAN_TELEPORT), 1f, 1.2f);
                                String successBar = plugin.getConfigFile().getString("tpa.messages.actionbar-success",
                                        "&a✔ Teleported to &b{player}").replace("{player}", requester.getName());
                                current.sendActionBar(Messages.colorComponent(successBar));
                                String msg = plugin.getConfigFile().getString("tpa.messages.teleported",
                                        "&7Teleported to &b%player%&7!").replace("%player%", requester.getName());
                                Messages.send(current, msg);
                                if (req != null && req.isOnline()) {
                                    Messages.send(req, plugin.getConfigFile().getString("tpa.messages.tpahere-accepted",
                                            "&b%player% &7teleported to you!").replace("%player%", current.getName()));
                                }
                            });
                        }
                    });
                }
            }
        };

        pendingCountdowns.put(targetUUID, countdownTask);
        countdownTask.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    public boolean denyRequest(Player target) {
        UUID requesterId = requests.remove(target.getUniqueId());
        UUID hereRequesterId = hereRequests.remove(target.getUniqueId());
        UUID actualRequester = requesterId != null ? requesterId : hereRequesterId;
        if (actualRequester == null) return false;
        cancelExpiry(target.getUniqueId());

        BukkitRunnable countdown = pendingCountdowns.remove(actualRequester);
        if (countdown != null) countdown.cancel();
        return true;
    }

    public boolean hasPendingRequest(UUID targetId) {
        return requests.containsKey(targetId);
    }

    public UUID getRequester(UUID targetId) {
        return requests.get(targetId);
    }

    private void cancelExpiry(UUID targetId) {
        BukkitTask task = expiryTasks.remove(targetId);
        if (task != null) task.cancel();
    }

    private boolean hasMoved(Location origin, Location current) {
        if (origin == null || current == null) return true;
        if (origin.getWorld() == null || current.getWorld() == null) return true;
        if (!Objects.equals(origin.getWorld(), current.getWorld())) return true;
        return origin.distanceSquared(current) > MOVEMENT_TOLERANCE_SQUARED;
    }

    public void shutdown() {
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        pendingCountdowns.values().forEach(BukkitRunnable::cancel);
        pendingCountdowns.clear();
        requests.clear();
        hereRequests.clear();
        cooldowns.clear();
    }
}
