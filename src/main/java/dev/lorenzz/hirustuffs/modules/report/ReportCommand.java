package dev.lorenzz.hirustuffs.modules.report;
import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
public class ReportCommand {
    private final ReportManager reportManager;
    public ReportCommand(ReportManager reportManager) {
        this.reportManager = reportManager;
    }
    @Command(name = "report", description = "Report a player to the staff", async = false)
    @Permission("hiru.report")
    public void report(@Sender Player player, @Named("player") String targetName, @Combined @Named("reason") String reason) {
        if (targetName == null || targetName.trim().isEmpty()) {
            Messages.send(player, "&#FF4444Usage: &f/report <player> <reason>");
            return;
        }
        if (reason == null || reason.trim().isEmpty()) {
            Messages.send(player, "&#FF4444Please provide a reason for the report!");
            return;
        }
        // Check if target exists (online)
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            targetName = target.getName(); // Normalize name casing
        }
        // Cannot report yourself
        if (target != null && target.getUniqueId().equals(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You cannot report yourself!");
            return;
        }
        boolean submitted = reportManager.submitReport(player, targetName, reason);
        if (submitted) {
            String separator = "&8&m                                                    ";
            Messages.sendRaw(player, "");
            Messages.sendRaw(player, separator);
            Messages.sendRaw(player, "&#FF4444&l  \u26A0 REPORT SUBMITTED");
            Messages.sendRaw(player, "");
            Messages.sendRaw(player, "&#8C8C8C  Player: &#FFD700" + targetName);
            Messages.sendRaw(player, "&#8C8C8C  Reason: &#E0E0E0" + reason);
            Messages.sendRaw(player, "");
            Messages.sendRaw(player, "&#55FF55  Your report has been sent to the staff team.");
            Messages.sendRaw(player, "&#8C8C8C  Thank you for helping keep the server safe!");
            Messages.sendRaw(player, separator);
            Messages.sendRaw(player, "");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
        }
    }
}
