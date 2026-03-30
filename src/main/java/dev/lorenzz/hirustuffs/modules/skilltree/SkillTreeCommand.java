package dev.lorenzz.hirustuffs.modules.skilltree;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.managers.SkillTreeManager;
import dev.lorenzz.hirustuffs.managers.SkillTreeManager.SkillDefinition;
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

public class SkillTreeCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "skills", aliases = {"skilltree", "skill"}, description = "Open the skill tree", async = false)
    public void skills(@Sender Player player) {
        openSkillTreeGUI(player);
    }

    @Command(name = "skillreset", aliases = {"skillsreset"}, description = "Reset all your skills", async = false)
    @Permission("hiru.skills.reset")
    public void reset(@Sender Player player) {
        SkillTreeManager stm = SkillTreeManager.getInstance();
        if (stm == null) { Messages.send(player, "&cSkill tree not available."); return; }

        Map<String, Integer> skills = stm.getPlayerSkills(player.getUniqueId());
        if (skills.isEmpty()) {
            Messages.send(player, "&#8C8C8CYou have no skills to reset.");
            return;
        }

        // Open confirmation GUI
        GUI.Builder builder = GUI.builder("&#FF4444&lReset Skills?", 3)
                .fill(Material.GRAY_STAINED_GLASS_PANE);

        double refundPercent = plugin.getConfigFile().getDouble("skilltree.reset-refund-percent", 50.0);

        builder.item(11, Material.LIME_WOOL, "&#55FF55&lCONFIRM RESET",
                List.of("",
                        "&#8C8C8CRefund: &#FFD700" + (int) refundPercent + "% &#8C8C8Cof shards invested",
                        "",
                        "&#FF4444⚠ This action cannot be undone!",
                        "",
                        "&#55FF55Click to confirm"),
                p -> {
                    int refund = stm.resetSkills(p.getUniqueId());
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 0.5f);
                    Messages.send(p, "&#55FF55Skills reset! Refunded &#FFD700" + refund + " Shards&#55FF55.");
                });

        builder.item(15, Material.RED_WOOL, "&#FF4444&lCANCEL",
                List.of("", "&#8C8C8CClick to cancel"),
                Player::closeInventory);

        builder.open(player);
    }

    private void openSkillTreeGUI(Player player) {
        SkillTreeManager stm = SkillTreeManager.getInstance();
        ShardManager sm = ShardManager.getInstance();
        if (stm == null || sm == null) {
            Messages.send(player, "&cSkill tree not available.");
            return;
        }

        Collection<SkillDefinition> skills = stm.getAllSkills();
        int rows = Math.min(Math.max((skills.size() / 7) + 3, 3), 6);

        int playerShards = sm.getShards(player.getUniqueId());

        GUI.Builder builder = GUI.builder("&#9C27B0&l⚡ Skill Tree &#8C8C8C| &#4FC3F7" + playerShards + " Shards", rows)
                .border(Material.PURPLE_STAINED_GLASS_PANE)
                .fill(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (SkillDefinition skill : skills) {
            if (slot >= (rows * 9) - 9) break;
            // Skip border slots
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }

            int level = stm.getPlayerLevel(player.getUniqueId(), skill.id());
            boolean maxed = level >= skill.maxLevel();
            int cost = maxed ? 0 : stm.getUpgradeCost(skill, level);
            boolean canAfford = playerShards >= cost;

            Material icon = Material.matchMaterial(skill.icon());
            if (icon == null) icon = Material.BOOK;

            // Build lore
            List<String> lore = new ArrayList<>();
            lore.add(Messages.color("&#8C8C8C" + skill.description()));
            lore.add("");

            // Level progress bar
            StringBuilder bar = new StringBuilder();
            for (int i = 1; i <= skill.maxLevel(); i++) {
                bar.append(i <= level ? "&#55FF55■ " : "&#555555■ ");
            }
            lore.add(Messages.color("&#E0E0E0Level: " + bar));
            lore.add(Messages.color("&#E0E0E0Progress: &#FFD700" + level + "&f/&#FFD700" + skill.maxLevel()));
            lore.add("");

            if (maxed) {
                lore.add(Messages.color("&#55FF55✔ MAXED OUT"));
            } else {
                String costColor = canAfford ? "&#55FF55" : "&#FF4444";
                lore.add(Messages.color("&#8C8C8CUpgrade Cost: " + costColor + cost + " Shards"));
                lore.add("");
                lore.add(Messages.color(canAfford ? "&#55FF55&lClick to upgrade!" : "&#FF4444✖ Not enough Shards"));
            }

            // Build item with enchant glow if has levels
            ItemStack itemStack = new ItemStack(icon);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Messages.color(skill.displayName() + " &#8C8C8C[Lv. " + level + "]"));
                meta.setLore(lore);
                if (level > 0) {
                    Enchantment glow = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
                    if (glow != null) meta.addEnchant(glow, 1, true);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                }
                itemStack.setItemMeta(meta);
            }

            final String skillId = skill.id();
            builder.item(slot, itemStack, p -> {
                if (stm.upgradeSkill(p.getUniqueId(), skillId)) {
                    int newLevel = stm.getPlayerLevel(p.getUniqueId(), skillId);
                    p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
                    Messages.send(p, "&#55FF55Upgraded &#FFD700" + Messages.color(skill.displayName()) +
                            " &#55FF55to level &#FFD700" + newLevel + "&#55FF55!");
                    // Reopen GUI to refresh
                    openSkillTreeGUI(p);
                } else {
                    int currentLvl = stm.getPlayerLevel(p.getUniqueId(), skillId);
                    if (currentLvl >= skill.maxLevel()) {
                        Messages.send(p, "&#FFD700This skill is already maxed out!");
                    } else {
                        Messages.send(p, "&#FF4444Not enough Shards to upgrade!");
                    }
                }
            });

            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // Reset button at bottom
        builder.item((rows * 9) - 5, Material.BARRIER, "&#FF4444&lReset All Skills",
                List.of("", "&#8C8C8CRefund a portion of shards invested", "&#FF4444Click to reset"),
                p -> reset(p));

        builder.open(player);
    }
}
