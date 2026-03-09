package dev.lorenzz.hirustuffs.modules.announcements;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.command.CommandSender;

public class AnnouncementCommand {

    @Command(name = "announce", aliases = {"announcements"}, description = "Manage announcements", async = false)
    @Permission("hiru.admin")
    public void announce(@Sender CommandSender sender) {
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

    @Subcommand(name = "reload", description = "Reload announcements configuration")
    @Permission("hiru.admin")
    public void reload(@Sender CommandSender sender) {
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

    @Subcommand(name = "toggle", description = "Toggle an announcement category")
    @Permission("hiru.admin")
    public void toggle(@Sender CommandSender sender, @Named("category") String categoryId) {
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
