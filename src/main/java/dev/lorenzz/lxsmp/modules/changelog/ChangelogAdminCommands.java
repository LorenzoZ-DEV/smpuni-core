package dev.lorenzz.lxsmp.modules.changelog;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.lxsmp.utils.GUI;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandAlias("changelogadmin|cladmin")
@CommandPermission("solum.admin")
@Description("Manage changelogs")
public class ChangelogAdminCommands extends BaseCommand {

    @Default
    public void changelogAdmin(CommandSender sender) {
        Messages.sendRaw(sender, "&8&m---------&8[ &#4FC3F7&lChangelog Admin &8]&8&m---------");
        Messages.sendRaw(sender, "&#4FC3F7/changelogadmin add <name> <version> <changes...>");
        Messages.sendRaw(sender, "  &7Adds a new changelog. Separate changes with |");
        Messages.sendRaw(sender, "  &7Example: /changelogadmin add Update 1.2 Added shop|Fixed bugs|New spawn");
        Messages.sendRaw(sender, "&#4FC3F7/changelogadmin remove <id> &7- Remove a changelog by ID");
        Messages.sendRaw(sender, "&#4FC3F7/changelogadmin list &7- Open changelog admin GUI");
        Messages.sendRaw(sender, "&8&m-----------------------------------------");
    }

    @Subcommand("add")
    @Description("Add a new changelog")
    public void changelogAdd(CommandSender sender, String name, String version, String[] changesArgs) {
        ChangelogManager cm = ChangelogManager.getInstance();
        if (cm == null) {
            Messages.send(sender, "&cChangelog system not available.");
            return;
        }

        String changesRaw = String.join(" ", changesArgs);
        List<String> changes = Arrays.stream(changesRaw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (changes.isEmpty()) {
            Messages.send(sender, "&cYou must provide at least one change! Separate with |");
            return;
        }

        cm.addChangelog(name, version, changes);
        Messages.send(sender, "&#55FF55Changelog &#FFD700" + name + " v" + version + " &#55FF55added with " + changes.size() + " change(s).");
    }

    @Subcommand("remove")
    @Description("Remove a changelog by ID")
    public void changelogRemove(CommandSender sender, String id) {
        ChangelogManager cm = ChangelogManager.getInstance();
        if (cm == null) {
            Messages.send(sender, "&cChangelog system not available.");
            return;
        }

        if (cm.removeChangelog(id)) {
            Messages.send(sender, "&#55FF55Changelog #" + id + " removed.");
        } else {
            Messages.send(sender, "&cChangelog #" + id + " not found.");
        }
    }

    @Subcommand("list")
    @Description("Open changelog admin GUI")
    public void changelogList(Player player) {
        ChangelogManager cm = ChangelogManager.getInstance();
        if (cm == null) {
            Messages.send(player, "&cChangelog system not available.");
            return;
        }
        openAdminGUI(player);
    }

    private void openAdminGUI(Player player) {
        ChangelogManager cm = ChangelogManager.getInstance();
        List<ChangelogManager.ChangelogEntry> entries = cm.getChangelogs();

        if (entries.isEmpty()) {
            Messages.send(player, "&#8C8C8CNo changelogs found.");
            return;
        }

        int rows = Math.min(Math.max((entries.size() / 7) + 3, 3), 6);

        GUI.Builder builder = GUI.builder("&#FF4444&l⚙ Changelog Admin", rows)
                .border(Material.RED_STAINED_GLASS_PANE)
                .fill(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (ChangelogManager.ChangelogEntry entry : entries) {
            if (slot >= (rows * 9) - 9) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&#8C8C8CVersion: &#4FC3F7" + entry.version());
            lore.add("&#8C8C8CDate: &#E0E0E0" + entry.date());
            lore.add("&#8C8C8CID: &#FFD700#" + entry.id());
            lore.add("");
            lore.add("&#FFD700&lChanges:");
            for (String change : entry.changes()) {
                lore.add("&#8C8C8C- &#E0E0E0" + change);
            }
            lore.add("");
            lore.add("&#FF4444&lClick to remove this changelog");

            final String entryId = entry.id();
            final String entryName = entry.name();
            builder.item(slot, Material.WRITABLE_BOOK,
                    "&#4FC3F7&l" + entry.name(),
                    lore,
                    p -> {
                        if (cm.removeChangelog(entryId)) {
                            Messages.send(p, "&#55FF55Changelog &#FFD700" + entryName + " &#55FF55(#" + entryId + ") removed.");
                            openAdminGUI(p);
                        }
                    });

            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        builder.open(player);
    }
}

