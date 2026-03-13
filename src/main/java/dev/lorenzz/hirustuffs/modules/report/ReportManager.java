package dev.lorenzz.hirustuffs.modules.report;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
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

public class ReportManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Hiru plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private File logFile;

    public ReportManager(Hiru plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            File logsDir = new File(plugin.getDataFolder(), "logs");
            Files.createDirectories(logsDir.toPath());
            logFile = new File(logsDir, "reports.log");
            if (!logFile.exists()) logFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "[Hiru] Failed to create reports log file", e);
        }
    }

    public boolean submitReport(Player reporter, String targetName, String reason) {
        int cooldownSeconds = plugin.getConfigFile().getInt("report.cooldown-seconds", 60);
        long remaining = getRemainingCooldown(reporter.getUniqueId(), cooldownSeconds);
        if (remaining > 0 && !reporter.hasPermission("hiru.report.bypass")) {
            String msg = plugin.getConfigFile().getString("report.messages.cooldown",
                            "&#FF4444You must wait &#FFD700%seconds% &#FF4444seconds before reporting again.")
                    .replace("%seconds%", String.valueOf(remaining));
            Messages.send(reporter, msg);
            return false;
        }

        cooldowns.put(reporter.getUniqueId(), System.currentTimeMillis());

        // Log to file async
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logEntry = String.format("[%s] %s reported %s: %s",
                timestamp, reporter.getName(), targetName, reason);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
                pw.println(logEntry);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "[Hiru] Failed to write report log", e);
            }
        });

        // Notify online staff
        String staffNotify = plugin.getConfigFile().getString("report.messages.staff-notify",
                        "&#FF4444&l\u26A0 REPORT &#8C8C8C\u00BB &#FF6B6B%reporter% &#8C8C8Creported &#FFD700%target% &#8C8C8Cfor: &#E0E0E0%reason%")
                .replace("%reporter%", reporter.getName())
                .replace("%target%", targetName)
                .replace("%reason%", reason);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("hiru.report.receive")) {
                Messages.sendRaw(online, "");
                Messages.sendRaw(online, staffNotify);
                Messages.sendRaw(online, "");
            }
        }

        // Console log
        Bukkit.getConsoleSender().sendMessage(Messages.color(staffNotify));

        // Discord webhook
        sendDiscordReport(reporter, targetName, reason);

        return true;
    }

    private void sendDiscordReport(Player reporter, String targetName, String reason) {
        String webhookUrl = plugin.getConfigFile().getString("report.discord-webhook", "");
        if (webhookUrl.isEmpty()) return;

        try {
            String headUrl = "https://crafatar.com/avatars/" + reporter.getUniqueId() + "?overlay&size=64";

            // Find target UUID for avatar (if online)
            Player target = Bukkit.getPlayerExact(targetName);
            String targetHead = target != null
                    ? "https://crafatar.com/avatars/" + target.getUniqueId() + "?overlay&size=64"
                    : "https://crafatar.com/avatars/Steve?overlay&size=64";

            int onlineStaff = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("hiru.report.receive")) onlineStaff++;
            }

            Embed embed = Embed.builder()
                    .title("\u26A0\uFE0F Player Report")
                    .color(0xFF4444)
                    .field("Reporter", reporter.getName(), true)
                    .field("Reported Player", targetName, true)
                    .field("Reason", reason, false)
                    .field("Server", "SmpUni", true)
                    .field("Staff Online", String.valueOf(onlineStaff), true)
                    .footer(Footer.of("Hiru Report System"))
                    .timestampNow()
                    .build();

            WebhookPayload payload = WebhookPayload.builder()
                    .username("Hiru Reports")
                    .avatarUrl(headUrl)
                    .embed(embed)
                    .noMentions()
                    .build();

            Discord.webhook(webhookUrl).sendMessage(payload)
                    .exceptionally(ex -> {
                        plugin.getLogger().warning("[Hiru] Failed to send report to Discord: " + ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            plugin.getLogger().warning("[Hiru] Failed to send report webhook: " + e.getMessage());
        }
    }

    public long getRemainingCooldown(UUID uuid, int cooldownSeconds) {
        Long lastUse = cooldowns.get(uuid);
        if (lastUse == null) return 0;
        long elapsed = (System.currentTimeMillis() - lastUse) / 1000L;
        long remaining = cooldownSeconds - elapsed;
        return Math.max(0, remaining);
    }
}
