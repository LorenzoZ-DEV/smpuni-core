package dev.lorenzz.hirustuffs.modules.ad;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.common.annotations.Optional;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AdCommand {

    private final Hiru plugin = Hiru.getInstance();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // The two copy-paste texts (what players paste in Minehut lobby)
    private static final String AD_NO_VIP = "./ad uninet SMPUNI Season I has been released [SKILLS] [ECONOMY] [LK STAFF] [JOIN NOW]";
    private static final String AD_VIP = "./ad uninet &9&lSMPUNI &fSeason I has been released &d[SKILLS] &3[ECONOMY] &6[LK STAFF] &a[JOIN NOW]";

    @Command(name = "ad", description = "Get an ad message to copy-paste", async = false)
    public void ad(@Sender Player player, @Optional @Named("server") String server) {

        // Default to "uninet" if no argument
        if (server == null || server.isEmpty()) {
            server = "uninet";
        }

        if (!server.equalsIgnoreCase("uninet")) {
            Messages.send(player, "&cUsage: &f/ad uninet");
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

        // Read texts from config (with hardcoded defaults)
        String noVipText = plugin.getConfigFile().getString("ad.copy-novip", AD_NO_VIP);
        String vipText = plugin.getConfigFile().getString("ad.copy-vip", AD_VIP);

        // ── Build the UI ──

        Component separator = Messages.colorComponent("&8&m                                                    ");

        // Header
        Component header = Component.empty()
                .append(Component.text("  ⚡ ", TextColor.color(0xFF, 0x44, 0x44)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("SMPUNI", TextColor.color(0xFF, 0x44, 0x44)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" AD", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true));

        // Instructions
        Component instructions = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("» ", TextColor.color(0xFF, 0xAA, 0x00)))
                .append(Component.text("Click a button below to copy the ad,", NamedTextColor.GRAY));

        Component instructions2 = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("  ", TextColor.color(0xFF, 0xAA, 0x00)))
                .append(Component.text("then paste it in the ", NamedTextColor.GRAY))
                .append(Component.text("Minehut Lobby", TextColor.color(0x55, 0xFF, 0xFF)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" chat!", NamedTextColor.GRAY));

        // ── NO VIP button ──
        Component noVipButton = Component.empty()
                .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("  ✂ ", TextColor.color(0xFF, 0xAA, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("NO VIP", TextColor.color(0xFF, 0xAA, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" — ", NamedTextColor.GRAY))
                .append(Component.text("plain text (no colors)", NamedTextColor.GRAY))
                .clickEvent(ClickEvent.copyToClipboard(noVipText))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to copy (No VIP version)", TextColor.color(0xFF, 0xAA, 0x00))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text(noVipText, NamedTextColor.WHITE))
                ));

        // ── VIP button ──
        Component vipButton = Component.empty()
                .append(Component.text("  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("  ✂ ", TextColor.color(0x55, 0xFF, 0x55)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("VIP", TextColor.color(0x55, 0xFF, 0x55)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" — ", NamedTextColor.GRAY))
                .append(Component.text("with colors (Minehut VIP only)", NamedTextColor.GRAY))
                .clickEvent(ClickEvent.copyToClipboard(vipText))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Click to copy (VIP version with colors)", TextColor.color(0x55, 0xFF, 0x55))
                                .append(Component.newline())
                                .append(Component.newline())
                                .append(Component.text(vipText, NamedTextColor.WHITE))
                ));

        // Preview of what it looks like
        Component previewLabel = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("  ⓘ ", TextColor.color(0x4F, 0xC3, 0xF7)))
                .append(Component.text("VIP Preview:", NamedTextColor.GRAY));

        Component previewLine = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("    "))
                .append(Messages.colorComponent("&9&lSMPUNI &fSeason I has been released &d[SKILLS] &3[ECONOMY] &6[LK STAFF] &a[JOIN NOW]"));

        // Send all
        player.sendMessage(Component.empty());
        player.sendMessage(separator);
        player.sendMessage(header);
        player.sendMessage(Component.empty());
        player.sendMessage(instructions);
        player.sendMessage(instructions2);
        player.sendMessage(Component.empty());
        player.sendMessage(noVipButton);
        player.sendMessage(vipButton);
        player.sendMessage(Component.empty());
        player.sendMessage(previewLabel);
        player.sendMessage(previewLine);
        player.sendMessage(Component.empty());
        player.sendMessage(separator);
        player.sendMessage(Component.empty());

        // Start cooldown
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Confirmation
        String confirm = plugin.getConfigFile().getString("ad.messages.confirmed",
                "&aThanks for advertising! &7Next use in &e%minutes% minutes&7.");
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
