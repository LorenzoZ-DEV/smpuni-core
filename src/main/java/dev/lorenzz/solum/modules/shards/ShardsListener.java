package dev.lorenzz.solum.modules.shards;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.ShardManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class ShardsListener implements Listener {

    private final Solum plugin;

    public ShardsListener(Solum plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ShardManager sm = ShardManager.getInstance();
        if (sm == null) return;

        int amount = plugin.getConfigFile().getInt("shards.earn.mob-kill", 5);
        sm.addShards(killer.getUniqueId(), amount);

        String msg = plugin.getConfigFile().getString("shards.messages.earned", "&7Guadagnati &d%amount% Shards!")
                .replace("%amount%", String.valueOf(amount));
        Messages.send(killer, msg);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        ShardManager sm = ShardManager.getInstance();
        if (sm == null) return;

        // Check mining
        String blockType = block.getType().name();
        int miningAmount = plugin.getConfigFile().getInt("shards.earn.mining." + blockType, 0);

        // Check farming
        if (miningAmount == 0) {
            miningAmount = plugin.getConfigFile().getInt("shards.earn.farming." + blockType, 0);
        }

        if (miningAmount > 0) {
            sm.addShards(player.getUniqueId(), miningAmount);
            String msg = plugin.getConfigFile().getString("shards.messages.earned", "&7Guadagnati &d%amount% Shards!")
                    .replace("%amount%", String.valueOf(miningAmount));
            Messages.send(player, msg);
        }
    }
}
