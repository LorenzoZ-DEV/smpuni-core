package dev.lorenzz.solum.modules.easter;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class EasterListener implements Listener {

    private final EasterManager manager;
    private final Solum plugin;

    public EasterListener(EasterManager manager, Solum plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    /**
     * Send the Easter event broadcast to a player on join (delayed 3 seconds)
     * and spawn eggs around them (delayed 5 seconds to ensure chunks are loaded)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!manager.isActive()) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !manager.isActive()) return;

            int myEggs = manager.getPlayerEggs(player.getUniqueId());

            player.sendMessage(Messages.color(""));
            player.sendMessage(Messages.color("&#FFB6C1&l✦ &#FFFACD&lEASTER EGG HUNT &#FFB6C1&l✦"));
            player.sendMessage(Messages.color(""));
            player.sendMessage(Messages.color("&#F8E1E8The Easter Egg Hunt is active! &#D8A7D8&lFind the eggs!"));
            player.sendMessage(Messages.color("&#E8E8E8Your eggs: &#FFB6C1" + myEggs + " &#E8E8E8| Total found: &#A8E6CF" + manager.getTotalCollected() + "&#E8E8E8/&#A8E6CF" + manager.getTotalEggsGoal()));
            player.sendMessage(Messages.color("&#E8E8E8Time remaining: &#FFFACD" + manager.getTimeRemaining()));
            player.sendMessage(Messages.color("&#E8E8E8Use &#FFFACD/easter top &#E8E8E8to see the leaderboard!"));
            player.sendMessage(Messages.color(""));

            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.5f);
        }, 60L); // 3 second delay

        // Spawn eggs around the player after 5 seconds (chunks will be loaded by then)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !manager.isActive()) return;
            manager.spawnEggsAroundPlayer(player);
        }, 100L); // 5 second delay
    }

    /**
     * Handle right-click on dragon egg (collect the easter egg)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (!manager.isActive()) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.DRAGON_EGG) return;

        if (manager.isEggLocation(block.getLocation())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            manager.collectEgg(player, block.getLocation());
        }
    }

    /**
     * Prevent breaking easter eggs normally
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        if (!manager.isActive()) return;

        Block block = event.getBlock();
        if (block.getType() != Material.DRAGON_EGG) return;

        if (manager.isEggLocation(block.getLocation())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            manager.collectEgg(player, block.getLocation());
        }
    }
}
