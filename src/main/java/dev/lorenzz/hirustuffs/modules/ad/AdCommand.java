package dev.lorenzz.hirustuffs.modules.ad;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.common.annotations.Optional;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AdCommand {

    private final Hiru plugin = Hiru.getInstance();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    @Command(name = "ad", description = "Get an ad message to copy-paste", async = false)
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

        // Get all ad messages from config
        List<String> allMessages = plugin.getConfigFile().getStringList("ad.messages-list");
        if (allMessages.isEmpty()) {
            Messages.send(player, "&cNo ad messages configured!");
            return;
        }

        // Pick a random message
        String chosen = allMessages.get(ThreadLocalRandom.current().nextInt(allMessages.size()));

        // Show header + the copyable message to the player only
        String separator = "&8&m                                                    ";
        Messages.sendRaw(player, "");
        Messages.sendRaw(player, separator);
        Messages.sendRaw(player, "&#FFD700&l  ⚡ AD MESSAGE — Copy & paste this in chat!");
        Messages.sendRaw(player, "");
        Messages.sendRaw(player, "  &f" + chosen);
        Messages.sendRaw(player, "");
        Messages.sendRaw(player, separator);
        Messages.sendRaw(player, "");

        // Start cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Confirmation
        String confirm = plugin.getConfigFile().getString("ad.messages.confirmed",
                "&aThanks for advertising SmpUni! &7Next use in &e%minutes% minutes&7.");
        Messages.send(player, confirm.replace("%minutes%", String.valueOf(cooldownSeconds / 60))
                .replace("%seconds%", String.valueOf(cooldownSeconds)));
    }

    // ── Helpers ──


    private long getRemainingCooldown(UUID uuid, int cooldownSeconds) {
        Long lastUse = cooldowns.get(uuid);
        if (lastUse == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        long remaining = cooldownSeconds - elapsed;
        return Math.max(0, remaining);
    }
}
