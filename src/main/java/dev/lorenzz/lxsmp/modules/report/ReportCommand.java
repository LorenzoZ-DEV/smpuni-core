package dev.lorenzz.lxsmp.modules.report;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Single;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.entity.Player;

@CommandAlias("report")
@CommandPermission("solum.report")
@Description("Report a player for misconduct")
public class ReportCommand extends BaseCommand {

    private final ReportManager manager;
    private final LXSMP plugin = LXSMP.getInstance();

    public ReportCommand(ReportManager manager) {
        this.manager = manager;
    }

    @Default
    public void report(Player sender, @Single String target, String reason) {
        if (target.equalsIgnoreCase(sender.getName())) {
            Messages.send(sender, "&#FF4444You can't report yourself!");
            return;
        }
        if (reason == null || reason.trim().isEmpty()) {
            String usage = plugin.getConfigFile().getString("report.messages.usage",
                    "&#FF4444Usage: &#FFD700/report <player> <reason>");
            Messages.send(sender, usage);
            return;
        }

        if (manager.submitReport(sender, target, reason.trim())) {
            String confirmation = plugin.getConfigFile().getString("report.messages.submitted",
                            "&#55FF55Your report against &#FFD700%target% &#55FF55has been submitted.")
                    .replace("%target%", target);
            Messages.send(sender, confirmation);
        }
    }
}

