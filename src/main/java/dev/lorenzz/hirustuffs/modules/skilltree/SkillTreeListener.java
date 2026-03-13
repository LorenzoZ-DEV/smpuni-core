package dev.lorenzz.hirustuffs.modules.skilltree;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.managers.SkillTreeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Applies skill tree effects:
 * - Reapplies potion effects on join
 * - Mining boost: chance for double drops
 * - Farming boost: chance for double crop drops
 * - Combat boost: extra shards on mob kills
 */
public class SkillTreeListener implements Listener {

    private final Hiru plugin;

    public SkillTreeListener(Hiru plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        SkillTreeManager stm = SkillTreeManager.getInstance();
        if (stm == null) return;
        // Delay 10 ticks to apply after player is fully loaded
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline()) {
                stm.applySkillEffects(player);
            }
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SkillTreeManager stm = SkillTreeManager.getInstance();
        if (stm == null) return;
        stm.removeSkillEffects(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        SkillTreeManager stm = SkillTreeManager.getInstance();
        if (stm == null) return;

        Player player = event.getPlayer();
        String material = event.getBlock().getType().name();

        // Mining boost — chance for double ore drops
        if (isMiningBlock(material)) {
            double multiplier = stm.getMultiplier(player.getUniqueId(), "mining_boost");
            if (multiplier > 1.0) {
                double chance = multiplier - 1.0; // e.g. 1.3 → 30% chance
                if (Math.random() < chance) {
                    // Duplicate the drops
                    event.getBlock().getDrops(player.getInventory().getItemInMainHand()).forEach(drop -> {
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                    });
                }
            }
        }

        // Farming boost — chance for double crop drops
        if (isFarmingBlock(material)) {
            double multiplier = stm.getMultiplier(player.getUniqueId(), "farming_boost");
            if (multiplier > 1.0) {
                double chance = multiplier - 1.0;
                if (Math.random() < chance) {
                    event.getBlock().getDrops(player.getInventory().getItemInMainHand()).forEach(drop -> {
                        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        SkillTreeManager stm = SkillTreeManager.getInstance();
        ShardManager sm = ShardManager.getInstance();
        if (stm == null || sm == null) return;

        Player killer = event.getEntity().getKiller();

        // XP boost — multiplies dropped XP
        double xpMulti = stm.getMultiplier(killer.getUniqueId(), "xp_boost");
        if (xpMulti > 1.0) {
            int baseXp = event.getDroppedExp();
            event.setDroppedExp((int) (baseXp * xpMulti));
        }

        // Luck — bonus shards on mob kill
        double luckMulti = stm.getMultiplier(killer.getUniqueId(), "luck");
        if (luckMulti > 1.0) {
            int bonusShards = (int) ((luckMulti - 1.0) * 10); // e.g. 1.3 → 3 bonus shards
            if (bonusShards > 0) {
                sm.addShards(killer.getUniqueId(), bonusShards);
            }
        }
    }

    private boolean isMiningBlock(String name) {
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS") ||
                name.equals("AMETHYST_CLUSTER") || name.equals("NETHER_GOLD_ORE");
    }

    private boolean isFarmingBlock(String name) {
        return name.equals("WHEAT") || name.equals("CARROTS") || name.equals("POTATOES") ||
                name.equals("BEETROOTS") || name.equals("MELON") || name.equals("PUMPKIN") ||
                name.equals("SUGAR_CANE") || name.equals("CACTUS") || name.equals("SWEET_BERRY_BUSH") ||
                name.equals("NETHER_WART") || name.equals("COCOA");
    }
}
