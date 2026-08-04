package dev.lorenzz.lxsmp.modules.announcements;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.command.CommandSender;

@CommandAlias("announce|announcements")
@CommandPermission("solum.admin")
@Description("Manage announcements")
public class AnnouncementCommand extends BaseCommand {

    @Default
    public void announce(CommandSender sender) {
        Messages.sendRaw(sender, "");
        Messages.sendRaw(sender, "&#FFD700&l⚡ Announcement System");
        Messages.sendRaw(sender, "&8&m                                        ");

        AnnouncementManager manager = AnnouncementManager.getInstance();
        if (manager == null) {
            Messages.send(sender, "&cAnnouncement system is not available.");
            return;
        }

        for (AnnouncementManager.AnnouncementCategory cat : manager.getCategories()) {
            String status = cat.isEnabled() ? "&#55FF55ON" : "&#FF4444OFF";
            Messages.sendRaw(sender, " &8▸ " + Messages.color(cat.getPrefix()) + "&7(" + cat.getId() + ") &8- " + Messages.color(status)
                    + " &8- &7" + cat.getMessages().size() + " messages");
        }

        Messages.sendRaw(sender, "");
        Messages.sendRaw(sender, " &7/announce reload &8- &#8C8C8CReload announcements");
        Messages.sendRaw(sender, " &7/announce toggle <category> &8- &#8C8C8CToggle a category");
        Messages.sendRaw(sender, "&8&m                                        ");
    }

    @Subcommand("reload")
    @Description("Reload announcements configuration")
    public void reload(CommandSender sender) {
        AnnouncementManager manager = AnnouncementManager.getInstance();
        if (manager == null) {
            Messages.send(sender, "&cAnnouncement system is not available.");
            return;
        }

        manager.reload();
        int total = 0;
        for (AnnouncementManager.AnnouncementCategory cat : manager.getCategories()) {
            if (cat.isEnabled()) total += cat.getMessages().size();
        }
        Messages.send(sender, "&#55FF55Announcements reloaded! &f" + total + " &#55FF55active messages.");
    }

    @Subcommand("toggle")
    @Description("Toggle an announcement category")
    public void toggle(CommandSender sender, String categoryId) {
        AnnouncementManager manager = AnnouncementManager.getInstance();
        if (manager == null) {
            Messages.send(sender, "&cAnnouncement system is not available.");
            return;
        }

        if (!manager.categoryExists(categoryId)) {
            Messages.send(sender, "&#FF4444Category &f" + categoryId + " &#FF4444not found!");
            Messages.sendRaw(sender, "&7Available: " + String.join(", ",
                    manager.getCategories().stream().map(AnnouncementManager.AnnouncementCategory::getId).toList()));
            return;
        }

        boolean newState = manager.toggleCategory(categoryId);
        String status = newState ? "&#55FF55enabled" : "&#FF4444disabled";
        Messages.send(sender, "&7Category &f" + categoryId + " &7is now " + Messages.color(status) + "&7.");
    }
}

