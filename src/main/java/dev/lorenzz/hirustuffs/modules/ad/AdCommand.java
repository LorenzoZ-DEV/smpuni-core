package dev.lorenzz.hirustuffs.modules.ad;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.common.annotations.Optional;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AdCommand {

    private final Hiru plugin = Hiru.getInstance();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    @Command(name = "ad", description = "Advertise SmpUni on Minehut", async = false)
    public void ad(@Sender Player player, @Optional @Named("server") String server) {


        // Default to "smpuni" if no argument
        if (server == null || server.isEmpty()) {
            server = "smpuni";
        }

        if (!server.equalsIgnoreCase("smpuni")) {
            Messages.send(player, "&cUsage: &f/ad smpuni");
            return;
        }

        // Cooldown check
        int cooldownSeconds = plugin.getConfigFile().getInt("ad.cooldown-seconds", 300);
        long remaining = getRemainingCooldown(player.getUniqueId(), cooldownSeconds);
        if (remaining > 0 && !player.hasPermission("hiru.ad.bypass")) {
            String msg = plugin.getConfigFile().getString("ad.messages.cooldown",
                            "&cYou can use /ad again in &e%seconds% &cseconds.")
                    .replace("%seconds%", String.valueOf(remaining));
            Messages.send(player, msg);
            return;
        }

        // Pick a style based on weighted random
        int standardWeight = plugin.getConfigFile().getInt("ad.weights.standard", 10);
        int coloredWeight = plugin.getConfigFile().getInt("ad.weights.colored", 70);
        int boxWeight = plugin.getConfigFile().getInt("ad.weights.box", 20);

        String style = pickWeightedStyle(standardWeight, coloredWeight, boxWeight);

        // Get messages for the selected style
        List<String> messages = plugin.getConfigFile().getStringList("ad.styles." + style);
        if (messages.isEmpty()) {
            Messages.send(player, "&cNo ad messages configured for style: " + style);
            return;
        }

        // Pick a random message
        String chosen = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));

        // Broadcast
        if (style.equals("box")) {
            // Box style: multiple lines separated by \n in a single entry
            String separator = plugin.getConfigFile().getString("ad.box-separator",
                    "&8&m                                                    ");
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(Messages.color(""));
                online.sendMessage(Messages.color(separator));
                for (String line : chosen.split("\\\\n")) {
                    online.sendMessage(Messages.color(line));
                }
                online.sendMessage(Messages.color(separator));
                online.sendMessage(Messages.color(""));
            }
        } else {
            // Single-line broadcast
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(Messages.color(chosen));
            }
        }

        // Start cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Confirmation to the sender
        String confirm = plugin.getConfigFile().getString("ad.messages.confirmed",
                "&aThanks for advertising SmpUni! &7Next use in &e%minutes% minutes&7.");
        Messages.send(player, confirm.replace("%minutes%", String.valueOf(cooldownSeconds / 60))
                .replace("%seconds%", String.valueOf(cooldownSeconds)));
    }


    // ── Helpers ──

    private String pickWeightedStyle(int standardWeight, int coloredWeight, int boxWeight) {
        int total = standardWeight + coloredWeight + boxWeight;
        int roll = ThreadLocalRandom.current().nextInt(total);

        if (roll < standardWeight) return "standard";
        if (roll < standardWeight + coloredWeight) return "colored";
        return "box";
    }

    private long getRemainingCooldown(UUID uuid, int cooldownSeconds) {
        Long lastUse = cooldowns.get(uuid);
        if (lastUse == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        long remaining = cooldownSeconds - elapsed;
        return Math.max(0, remaining);
    }
}
