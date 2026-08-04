package dev.lorenzz.lxsmp.modules.bounty;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.BountyManager;
import dev.lorenzz.lxsmp.managers.BountyManager.Bounty;
import dev.lorenzz.lxsmp.managers.ShardManager;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class BountyListener implements Listener {

    private final LXSMP plugin;

    public BountyListener(LXSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        BountyManager bm = BountyManager.getInstance();
        if (bm == null || !bm.hasBounty(victim.getUniqueId())) return;

        Bounty bounty = bm.claimBounty(victim.getUniqueId());
        if (bounty == null) return;

        ShardManager sm = ShardManager.getInstance();
        if (sm != null) {
            sm.addShards(killer.getUniqueId(), bounty.amount());
        }

        Messages.send(killer, "&#55FF55&l⚔ BOUNTY CLAIMED! &#E0E0E0You received &#FFD700" +
                bounty.amount() + " Shards &#E0E0E0for killing &#FF6B6B" + victim.getName());

        Bukkit.broadcast(Messages.colorComponent(
                "&#55FF55&l⚔ BOUNTY CLAIMED &#8C8C8C| &#FFD700" + killer.getName() +
                " &#E0E0E0claimed &#FFD700" + bounty.amount() + " Shards &#E0E0E0bounty on &#FF6B6B" + victim.getName()));
    }
}
