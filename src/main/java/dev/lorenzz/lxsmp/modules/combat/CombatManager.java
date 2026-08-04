package dev.lorenzz.lxsmp.modules.combat;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatManager {

    private static CombatManager INSTANCE;

    private final LXSMP plugin;
    private final Map<UUID, Integer> combatSeconds = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();
    private BukkitTask actionBarTask;

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

    public CombatManager(LXSMP plugin) {
        this.plugin = plugin;
        INSTANCE = this;
        reload();
        startActionBarTask();
    }

    public static CombatManager getInstance() {
        return INSTANCE;
    }

    public void reload() {
        combatDurationSeconds = plugin.getConfigFile().getInt("combat.tag-duration-seconds");
        punishLogout = plugin.getConfigFile().getBoolean("combat.punish-logout");
        dropItemsOnLogout = plugin.getConfigFile().getBoolean("combat.drop-items-on-logout");
        blockCommands = plugin.getConfigFile().getBoolean("combat.block-commands");
        blockEnderPearl = plugin.getConfigFile().getBoolean("combat.block-ender-pearl");
        blockChorusFruit = plugin.getConfigFile().getBoolean("combat.block-chorus-fruit");
        blockTeleport = plugin.getConfigFile().getBoolean("combat.block-teleport");
        blockFlying = plugin.getConfigFile().getBoolean("combat.block-flying");
        blockedCommands = plugin.getConfigFile().getStringList("combat.blocked-commands");
        allowedCommands = plugin.getConfigFile().getStringList("combat.allowed-commands");

        if (blockedCommands.isEmpty()) {
            blockedCommands = List.of(
                    "/home", "/sethome", "/tpa", "/tpaccept", "/spawn",
                    "/rtp", "/back", "/warp", "/tp", "/fly", "/afk",
                    "/logout", "/hub", "/leave", "/kit"
            );
        }
    }

    public void tagPlayer(UUID attacker, UUID victim) {
        boolean attackerNewlyTagged = !isInCombat(attacker);
        boolean victimNewlyTagged = !isInCombat(victim);

        combatSeconds.put(attacker, combatDurationSeconds);
        combatSeconds.put(victim, combatDurationSeconds);
        lastAttacker.put(victim, attacker);
        lastAttacker.put(attacker, victim);

        Player attackerPlayer = Bukkit.getPlayer(attacker);
        Player victimPlayer = Bukkit.getPlayer(victim);

        if (attackerNewlyTagged && attackerPlayer != null && attackerPlayer.isOnline()) {
            Messages.send(attackerPlayer, getMessage("tagged"));
            if (blockFlying && attackerPlayer.isFlying()) {
                attackerPlayer.setFlying(false);
                attackerPlayer.setAllowFlight(false);
                Messages.send(attackerPlayer, getMessage("fly-disabled"));
            }
        }

        if (victimNewlyTagged && victimPlayer != null && victimPlayer.isOnline()) {
            Messages.send(victimPlayer, getMessage("tagged"));
            if (blockFlying && victimPlayer.isFlying()) {
                victimPlayer.setFlying(false);
                victimPlayer.setAllowFlight(false);
                Messages.send(victimPlayer, getMessage("fly-disabled"));
            }
        }
    }

    public void untagPlayer(UUID uuid) {
        combatSeconds.remove(uuid);
        lastAttacker.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            Messages.send(player, getMessage("leave"));
        }
    }

    public boolean isInCombat(UUID uuid) {
        Integer seconds = combatSeconds.get(uuid);
        return seconds != null && seconds > 0;
    }

    public long getRemainingSeconds(UUID uuid) {
        Integer seconds = combatSeconds.get(uuid);
        return seconds == null ? 0 : Math.max(0, seconds);
    }

    public UUID getLastAttacker(UUID victim) {
        return lastAttacker.get(victim);
    }

    public void handleCombatLogout(Player player) {
        if (!punishLogout) return;
        if (!isInCombat(player.getUniqueId())) return;
        if (player.hasPermission("solum.combat.bypass")) return;

        UUID attackerUUID = lastAttacker.get(player.getUniqueId());

        if (dropItemsOnLogout) {
            player.getInventory().forEach(item -> {
                if (item != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            });
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
        }

        player.setHealth(0);

        String killerName = "Unknown";
        if (attackerUUID != null) {
            Player attacker = Bukkit.getPlayer(attackerUUID);
            if (attacker != null) {
                killerName = attacker.getName();
                untagPlayer(attackerUUID);
            }
        }

        String broadcastMsg = getMessage("combat-log-broadcast")
                .replace("%player%", player.getName())
                .replace("%attacker%", killerName);

        for (Player online : Bukkit.getOnlinePlayers()) {
            Messages.sendRaw(online, broadcastMsg);
        }

        combatSeconds.remove(player.getUniqueId());
        lastAttacker.remove(player.getUniqueId());
    }

    public boolean isCommandBlocked(String command) {
        if (!blockCommands) return false;
        String cmd = command.toLowerCase().split(" ")[0];

        for (String allowed : allowedCommands) {
            if (cmd.equalsIgnoreCase(allowed.toLowerCase())) return false;
        }

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

    private void startActionBarTask() {
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, Integer>> iterator = combatSeconds.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                UUID uuid = entry.getKey();
                int seconds = entry.getValue();
                Player player = Bukkit.getPlayer(uuid);

                if (player == null || !player.isOnline()) {
                    iterator.remove();
                    lastAttacker.remove(uuid);
                    continue;
                }

                if (seconds > 0) {
                    entry.setValue(seconds - 1);
                    String bar = buildProgressBar(seconds, combatDurationSeconds);
                    String actionBarText = getMessage("actionbar")
                            .replace("%bar%", bar)
                            .replace("{seconds}", String.valueOf(seconds))
                            .replace("%seconds%", String.valueOf(seconds));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(Messages.color(actionBarText)));
                } else {
                    iterator.remove();
                    lastAttacker.remove(uuid);
                    Messages.send(player, getMessage("leave"));
                }
            }
        }, 0L, 20L);
    }

    private String buildProgressBar(long remaining, int total) {
        int barLength = 20;
        double ratio = (double) remaining / (double) total;
        int filled = (int) Math.round(ratio * barLength);
        int empty = barLength - filled;

        StringBuilder bar = new StringBuilder();
        String filledColor;
        if (ratio > 0.6) {
            filledColor = "&#FF4444";
        } else if (ratio > 0.3) {
            filledColor = "&#FFD700";
        } else {
            filledColor = "&#55FF55";
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
        combatSeconds.clear();
        lastAttacker.clear();
        INSTANCE = null;
    }

    public String getMessage(String key) {
        return plugin.getConfigFile().getString("combat.messages." + key, "");
    }

    public int getCombatDurationSeconds() {
        return combatDurationSeconds;
    }
}
