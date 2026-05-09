package dev.lorenzz.solum.modules.combat;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private static CombatManager INSTANCE;

    private final Solum plugin;
    private final Map<UUID, Long> combatTagExpiry = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();
    private BukkitTask actionBarTask;

    // Config values
    private int combatDurationSeconds;
    private boolean punishLogout;
    private boolean dropItemsOnLogout;
    private boolean blockCommands;
    private boolean blockEnderPearl;
    private boolean blockChorusFruit;
    private boolean blockTeleport;
    private boolean blockFlying;
    private List<String> blockedCommands;
    private List<String> allowedCommands;

    public CombatManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
        reload();
        startActionBarTask();
    }

    public static CombatManager getInstance() {
        return INSTANCE;
    }

    public void reload() {
        combatDurationSeconds = plugin.getConfigFile().getInt("combat.tag-duration-seconds", 15);
        punishLogout = plugin.getConfigFile().getBoolean("combat.punish-logout", true);
        dropItemsOnLogout = plugin.getConfigFile().getBoolean("combat.drop-items-on-logout", true);
        blockCommands = plugin.getConfigFile().getBoolean("combat.block-commands", true);
        blockEnderPearl = plugin.getConfigFile().getBoolean("combat.block-ender-pearl", false);
        blockChorusFruit = plugin.getConfigFile().getBoolean("combat.block-chorus-fruit", true);
        blockTeleport = plugin.getConfigFile().getBoolean("combat.block-teleport", true);
        blockFlying = plugin.getConfigFile().getBoolean("combat.block-flying", true);
        blockedCommands = plugin.getConfigFile().getStringList("combat.blocked-commands");
        allowedCommands = plugin.getConfigFile().getStringList("combat.allowed-commands");

        // Defaults if lists empty
        if (blockedCommands.isEmpty()) {
            blockedCommands = List.of(
                    "/home", "/sethome", "/tpa", "/tpaccept", "/spawn",
                    "/rtp", "/back", "/warp", "/tp", "/fly", "/afk",
                    "/logout", "/hub", "/leave", "/kit"
            );
        }
    }

    /**
     * Tags both players in combat.
     */
    public void tagPlayer(UUID attacker, UUID victim) {
        long expiryTime = System.currentTimeMillis() + (combatDurationSeconds * 1000L);
        boolean attackerNewlyTagged = !isInCombat(attacker);
        boolean victimNewlyTagged = !isInCombat(victim);

        combatTagExpiry.put(attacker, expiryTime);
        combatTagExpiry.put(victim, expiryTime);
        lastAttacker.put(victim, attacker);
        lastAttacker.put(attacker, victim);

        Player attackerPlayer = Bukkit.getPlayer(attacker);
        Player victimPlayer = Bukkit.getPlayer(victim);

        if (attackerNewlyTagged && attackerPlayer != null && attackerPlayer.isOnline()) {
            Messages.send(attackerPlayer, getMessage("tagged",
                    "&#FF4444⚔ You are now in combat! &7Do not log out."));
            // Disable flying if enabled
            if (blockFlying && attackerPlayer.isFlying()) {
                attackerPlayer.setFlying(false);
                attackerPlayer.setAllowFlight(false);
                Messages.send(attackerPlayer, getMessage("fly-disabled",
                        "&#FF4444Flight disabled — you are in combat!"));
            }
        }

        if (victimNewlyTagged && victimPlayer != null && victimPlayer.isOnline()) {
            Messages.send(victimPlayer, getMessage("tagged",
                    "&#FF4444⚔ You are now in combat! &7Do not log out."));
            if (blockFlying && victimPlayer.isFlying()) {
                victimPlayer.setFlying(false);
                victimPlayer.setAllowFlight(false);
                Messages.send(victimPlayer, getMessage("fly-disabled",
                        "&#FF4444Flight disabled — you are in combat!"));
            }
        }
    }

    /**
     * Remove combat tag for a player.
     */
    public void untagPlayer(UUID uuid) {
        combatTagExpiry.remove(uuid);
        lastAttacker.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            Messages.send(player, getMessage("expired",
                    "&#55FF55✔ You are no longer in combat."));
        }
    }

    public boolean isInCombat(UUID uuid) {
        Long expiry = combatTagExpiry.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            combatTagExpiry.remove(uuid);
            lastAttacker.remove(uuid);
            return false;
        }
        return true;
    }

    public long getRemainingSeconds(UUID uuid) {
        Long expiry = combatTagExpiry.get(uuid);
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    public UUID getLastAttacker(UUID victim) {
        return lastAttacker.get(victim);
    }

    /**
     * Handle combat logout — kill the player, drop inventory, notify.
     */
    public void handleCombatLogout(Player player) {
        if (!punishLogout) return;
        if (!isInCombat(player.getUniqueId())) return;
        if (player.hasPermission("solum.combat.bypass")) return;

        UUID attackerUUID = lastAttacker.get(player.getUniqueId());

        // Drop items at their location
        if (dropItemsOnLogout) {
            player.getInventory().forEach(item -> {
                if (item != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            });
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }

        // Kill the player
        player.setHealth(0);

        // Broadcast message
        String killerName = "Unknown";
        if (attackerUUID != null) {
            Player attacker = Bukkit.getPlayer(attackerUUID);
            if (attacker != null) {
                killerName = attacker.getName();
                // Untag the attacker since the fight is over
                untagPlayer(attackerUUID);
            }
        }

        String broadcastMsg = getMessage("combat-log-broadcast",
                "&#FF4444⚠ &#FF6B6B%player% &#8C8C8Clogged out while in combat with &#55FF55%attacker%&#8C8C8C!")
                .replace("%player%", player.getName())
                .replace("%attacker%", killerName);

        for (Player online : Bukkit.getOnlinePlayers()) {
            Messages.sendRaw(online, broadcastMsg);
        }

        combatTagExpiry.remove(player.getUniqueId());
        lastAttacker.remove(player.getUniqueId());
    }

    /**
     * Check if a command is blocked during combat.
     */
    public boolean isCommandBlocked(String command) {
        if (!blockCommands) return false;
        String cmd = command.toLowerCase().split(" ")[0];

        // Check allowed commands first (whitelist)
        for (String allowed : allowedCommands) {
            if (cmd.equalsIgnoreCase(allowed.toLowerCase())) return false;
        }

        // Check blocked commands
        for (String blocked : blockedCommands) {
            if (cmd.equalsIgnoreCase(blocked.toLowerCase()) || cmd.startsWith(blocked.toLowerCase() + " ")) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnderPearlBlocked() {
        return blockEnderPearl;
    }

    public boolean isChorusFruitBlocked() {
        return blockChorusFruit;
    }

    public boolean isTeleportBlocked() {
        return blockTeleport;
    }

    public boolean isFlyingBlocked() {
        return blockFlying;
    }

    /**
     * Action bar task to show combat timer to all tagged players.
     */
    private void startActionBarTask() {
        actionBarTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Iterator<Map.Entry<UUID, Long>> it = combatTagExpiry.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Long> entry = it.next();
                UUID uuid = entry.getKey();
                long expiry = entry.getValue();
                long now = System.currentTimeMillis();

                if (now >= expiry) {
                    it.remove();
                    lastAttacker.remove(uuid);
                    // Send expiry message on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            Messages.send(player, getMessage("expired",
                                    "&#55FF55✔ You are no longer in combat."));
                        }
                    });
                    continue;
                }

                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    long remainingSeconds = (expiry - now) / 1000L;
                    // Build a progress bar
                    String bar = buildProgressBar(remainingSeconds, combatDurationSeconds);
                    String actionBarText = getMessage("actionbar",
                            "&#FF4444⚔ COMBAT &#8C8C8C» %bar% &#FF6B6B%seconds%s")
                            .replace("%bar%", bar)
                            .replace("%seconds%", String.valueOf(remainingSeconds));

                    // Send action bar (sync not required for action bar via spigot API)
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(Messages.color(actionBarText)));
                }
            }
        }, 0L, 5L); // Every 0.25 seconds for smooth updates
    }

    private String buildProgressBar(long remaining, int total) {
        int barLength = 20;
        double ratio = (double) remaining / (double) total;
        int filled = (int) Math.round(ratio * barLength);
        int empty = barLength - filled;

        StringBuilder bar = new StringBuilder();
        // Color based on remaining time
        String filledColor;
        if (ratio > 0.6) {
            filledColor = "&#FF4444"; // Red — most time left (still dangerous)
        } else if (ratio > 0.3) {
            filledColor = "&#FFD700"; // Yellow — halfway
        } else {
            filledColor = "&#55FF55"; // Green — almost free
        }

        bar.append(filledColor);
        bar.append("█".repeat(Math.max(0, filled)));
        bar.append("&8");
        bar.append("░".repeat(Math.max(0, empty)));
        return bar.toString();
    }

    public void shutdown() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        combatTagExpiry.clear();
        lastAttacker.clear();
        INSTANCE = null;
    }

    public String getMessage(String key, String def) {
        return plugin.getConfigFile().getString("combat.messages." + key, def);
    }

    public int getCombatDurationSeconds() {
        return combatDurationSeconds;
    }
}
