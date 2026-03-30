package dev.lorenzz.hirustuffs.modules.changelog;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChangelogCommands {

    @Command(name = "changelog", aliases = {"changelogs"}, description = "View server changelogs", async = false)
    public void changelog(@Sender Player player) {
        ChangelogManager cm = ChangelogManager.getInstance();
        if (cm == null) {
            Messages.send(player, "&cChangelog system not available.");
            return;
        }

        List<ChangelogManager.ChangelogEntry> entries = cm.getChangelogs();
        if (entries.isEmpty()) {
            Messages.send(player, "&#8C8C8CNo changelogs available yet.");
            return;
        }

        int rows = Math.min(Math.max((entries.size() / 7) + 3, 3), 6);

        GUI.Builder builder = GUI.builder("&#0053FF&l⟐ Changelogs", rows)
                .border(Material.BLUE_STAINED_GLASS_PANE)
                .fill(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (ChangelogManager.ChangelogEntry entry : entries) {
            if (slot >= (rows * 9) - 9) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&#8C8C8CVersion: &#4FC3F7" + entry.version());
            lore.add("&#8C8C8CDate: &#E0E0E0" + entry.date());
            lore.add("");
            lore.add("&#FFD700&lChanges:");

            for (String change : entry.changes()) {
                lore.add("&#8C8C8C- &#E0E0E0" + change);
            }

            lore.add("");
            lore.add("&#8C8C8CChangelog #" + entry.id());

            builder.item(slot, Material.WRITABLE_BOOK,
                    "&#4FC3F7&l" + entry.name(),
                    lore);


            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        builder.open(player);
    }
}
