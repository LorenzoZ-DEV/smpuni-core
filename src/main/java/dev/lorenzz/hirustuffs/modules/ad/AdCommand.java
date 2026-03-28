package dev.lorenzz.hirustuffs.modules.ad;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.common.annotations.Optional;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

        // Build the copyable text: the ad message itself
        Audience audience = player;

        // Separator line
        Component separator = Messages.colorComponent("&8&m                                                    ");

        // Header
        Component header = Component.empty()
                .append(Component.text("  ⚡ ", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("AD MESSAGE", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" — ", NamedTextColor.GRAY))
                .append(Component.text("SmpUni", TextColor.color(0x55, 0xFF, 0xFF)).decoration(TextDecoration.BOLD, true));

        // The ad message preview (white)
        Component messagePreview = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("» ", TextColor.color(0xFF, 0xAA, 0x00)))
                .append(Messages.colorComponent("&f" + chosen));

        // Click-to-copy button
        Component clickButton = Component.empty()
                .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("  ✂ ", TextColor.color(0x55, 0xFF, 0x55)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("CLICK TO COPY", TextColor.color(0x55, 0xFF, 0x55)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" — ", NamedTextColor.GRAY))
                .append(Component.text("paste it in another server's chat!", NamedTextColor.GRAY))
                .clickEvent(ClickEvent.copyToClipboard(chosen))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to copy the ad message!", TextColor.color(0x55, 0xFF, 0x55))
                                .append(Component.newline())
                                .append(Component.text(chosen, NamedTextColor.WHITE))
                ));

        // Tip: /join SmpUni
        Component joinTip = Component.empty()
                .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("  ⓘ ", TextColor.color(0x4F, 0xC3, 0xF7)))
                .append(Component.text("Players can join with: ", NamedTextColor.GRAY))
                .append(Component.text("/join SmpUni", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.copyToClipboard("/join SmpUni"))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Click to copy ", NamedTextColor.GRAY)
                                        .append(Component.text("/join SmpUni", TextColor.color(0xFF, 0xD7, 0x00)))
                        ))
                );

        // Send all components
        audience.sendMessage(Component.empty());
        audience.sendMessage(separator);
        audience.sendMessage(header);
        audience.sendMessage(Component.empty());
        audience.sendMessage(messagePreview);
        audience.sendMessage(Component.empty());
        audience.sendMessage(clickButton);
        audience.sendMessage(joinTip);
        audience.sendMessage(Component.empty());
        audience.sendMessage(separator);
        audience.sendMessage(Component.empty());

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
