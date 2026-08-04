package dev.lorenzz.lxsmp.modules.request;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import dev.lorenzz.libs.discord.Discord;
import dev.lorenzz.libs.discord.builders.Embed;
import dev.lorenzz.libs.discord.builders.WebhookPayload;
import dev.lorenzz.libs.discord.builders.embeds.Footer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class RequestManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LXSMP plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private File logFile;

    public RequestManager(LXSMP plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File logsDir = new File(plugin.getDataFolder(), "logs");
            Files.createDirectories(logsDir.toPath());
            logFile = new File(logsDir, "requests.log");
            if (!logFile.exists()) logFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Solum] Failed to create requests log file", e);
        }
    }

    public boolean submitRequest(Player requester, String message) {
        int cooldownSeconds = plugin.getConfigFile().getInt("request.cooldown-seconds", 120);
        long remaining = getRemainingCooldown(requester.getUniqueId(), cooldownSeconds);
        if (remaining > 0 && !requester.hasPermission("solum.request.bypass")) {
            String msg = plugin.getConfigFile().getString("request.messages.cooldown",
                            "&#FF4444You must wait &#FFD700%seconds% &#FF4444seconds before sending another request.")
                    .replace("%seconds%", String.valueOf(remaining));
            Messages.send(requester, msg);
            return false;
        }

        cooldowns.put(requester.getUniqueId(), System.currentTimeMillis());

        // Log to file async
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logEntry = String.format("[%s] %s requested: %s",
                timestamp, requester.getName(), message);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
                pw.println(logEntry);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[Solum] Failed to write request log", e);
            }
        });

        // Notify online staff
        String staffNotify = plugin.getConfigFile().getString("request.messages.staff-notify",
                        "&#4FC3F7&l\u2709 REQUEST &#8C8C8C\u00BB &#4FC3F7%player% &#8C8C8Crequests: &#E0E0E0%message%")
                .replace("%player%", requester.getName())
                .replace("%message%", message);

        int staffCount = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("solum.request.receive")) {
                Messages.sendRaw(online, "");
                Messages.sendRaw(online, staffNotify);
                Messages.sendRaw(online, "");
                staffCount++;
            }
        }

        // Console log
        Bukkit.getConsoleSender().sendMessage(Messages.color(staffNotify));

        // Discord webhook
        sendDiscordRequest(requester, message, staffCount);

        return true;
    }

    private void sendDiscordRequest(Player requester, String message, int staffNotified) {
        String webhookUrl = plugin.getConfigFile().getString("request.discord-webhook", "");
        if (webhookUrl.isEmpty()) return;

        try {
            String headUrl = "https://crafatar.com/avatars/" + requester.getUniqueId() + "?overlay&size=64";

            Embed embed = Embed.builder()
                    .title("\u2709\uFE0F Staff Request")
                    .color(0x4FC3F7)
                    .field("Player", requester.getName(), true)
                    .field("Staff Notified", String.valueOf(staffNotified), true)
                    .field("Message", message, false)
                    .field("Server", "SmpUni", true)
                    .field("World", requester.getWorld().getName(), true)
                    .footer(Footer.of("Solum Request System"))
                    .timestampNow()
                    .build();

            WebhookPayload payload = WebhookPayload.builder()
                    .username("Solum Requests")
                    .avatarUrl(headUrl)
                    .embed(embed)
                    .noMentions()
                    .build();

            Discord.webhook(webhookUrl).sendMessage(payload)
                    .exceptionally(ex -> {
                        plugin.getLogger().warning("[Solum] Failed to send request to Discord: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            plugin.getLogger().warning("[Solum] Failed to send request webhook: " + e.getMessage());
        }
    }

    public int getOnlineStaffCount() {
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("solum.request.receive")) {
                count++;
            }
        }
        return count;
    }

    public long getRemainingCooldown(UUID uuid, int cooldownSeconds) {
        Long lastUse = cooldowns.get(uuid);
        if (lastUse == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        long remaining = cooldownSeconds - elapsed;
        return Math.max(0, remaining);
    }
}
