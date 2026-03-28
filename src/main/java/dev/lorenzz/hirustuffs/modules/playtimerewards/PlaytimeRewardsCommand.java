package dev.lorenzz.hirustuffs.modules.playtimerewards;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.PlaytimeRewardsManager;
import dev.lorenzz.hirustuffs.managers.PlaytimeRewardsManager.Milestone;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlaytimeRewardsCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "rewards", aliases = {"playtimerewards", "ptrewards"}, description = "Open playtime rewards", async = false)
    public void rewards(@Sender Player player) {
        openRewardsGUI(player);
    }

    private void openRewardsGUI(Player player) {
        PlaytimeRewardsManager prm = PlaytimeRewardsManager.getInstance();
        if (prm == null) { Messages.send(player, "&cRewards not available."); return; }

        List<Milestone> milestones = prm.getMilestones();
        int rows = Math.min(Math.max((milestones.size() / 7) + 3, 3), 6);

        long activeMinutes = prm.getActivePlaytimeMinutes(player.getUniqueId());
        String formattedTime = prm.getFormattedActivePlaytime(player.getUniqueId());

        GUI.Builder builder = GUI.builder(
                "&#FFD700&l⏱ Rewards &#8C8C8C| &#E0E0E0" + formattedTime + " played", rows)
                .border(Material.ORANGE_STAINED_GLASS_PANE)
                .fill(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (Milestone ms : milestones) {
            if (slot >= (rows * 9) - 9) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }

            boolean claimed = prm.hasClaimed(player.getUniqueId(), ms.minutes());
            boolean available = activeMinutes >= ms.minutes();

            Material icon = Material.matchMaterial(ms.icon());
            if (icon == null) icon = Material.CLOCK;

            // Determine status
            String statusLine;
            Material glassTint;
            if (claimed) {
                statusLine = "&#55FF55✔ Claimed";
                glassTint = Material.LIME_STAINED_GLASS_PANE;
            } else if (available) {
                statusLine = "&#FFD700⬇ Click to claim!";
                glassTint = Material.YELLOW_STAINED_GLASS_PANE;
            } else {
                long remaining = ms.minutes() - activeMinutes;
                statusLine = "&#FF4444✖ " + formatTime(remaining) + " remaining";
                glassTint = Material.RED_STAINED_GLASS_PANE;
            }

            // Build lore
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&#8C8C8CRequired: &#E0E0E0" + formatTime(ms.minutes())));
            lore.add("");
            if (ms.moneyReward() > 0) {
                lore.add(Messages.color("  &#FFD700$ " + ms.moneyReward() + " Money"));
            }
            if (ms.shardsReward() > 0) {
                lore.add(Messages.color("  &#4FC3F7★ " + ms.shardsReward() + " Shards"));
            }
            if (!ms.commands().isEmpty()) {
                lore.add(Messages.color("  &#CE93D8✦ Special Reward"));
            }
            lore.add("");
            lore.add(Messages.color(statusLine));

            ItemStack itemStack = new ItemStack(claimed ? glassTint : (available ? icon : Material.GRAY_DYE));
            if (available && !claimed) {
                itemStack = new ItemStack(icon);
            }

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Messages.color(ms.displayName()));
                meta.setLore(lore);
                if (available && !claimed) {
                    Enchantment glow = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
                    if (glow != null) meta.addEnchant(glow, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                itemStack.setItemMeta(meta);
            }

            final int milestoneMinutes = ms.minutes();
            builder.item(slot, itemStack, p -> {
                if (prm.claimMilestone(p, milestoneMinutes)) {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                    Messages.send(p, "&#55FF55Reward claimed! &#FFD700" + Messages.color(ms.displayName()));
                    openRewardsGUI(p); // Refresh GUI
                } else if (prm.hasClaimed(p.getUniqueId(), milestoneMinutes)) {
                    Messages.send(p, "&#8C8C8CYou already claimed this reward.");
                } else {
                    Messages.send(p, "&#FF4444You haven't played long enough yet!");
                }
            });

            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // Info item showing active vs total playtime
        builder.item((rows * 9) - 5, Material.CLOCK, "&#E0E0E0&lYour Playtime",
                List.of("",
                        Messages.color("&#8C8C8CActive Time: &#55FF55" + formattedTime),
                        "",
                        Messages.color("&#8C8C8CAFK time does NOT count"),
                        Messages.color("&#8C8C8Ctowards rewards!")),
                null);

        builder.open(player);
    }

    private String formatTime(long minutes) {
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        long mins = minutes % 60;
        if (mins == 0) return hours + "h";
        return hours + "h " + mins + "m";
    }
}
