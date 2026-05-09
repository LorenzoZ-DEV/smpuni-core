package dev.lorenzz.solum.modules.kits;

import dev.lorenzz.solum.Solum;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles first-join auto-kit granting (starter kit).
 */
public class KitListener implements Listener {

    private final Solum plugin;

    public KitListener(Solum plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Only process if kits module is enabled
        KitManager kitManager = KitManager.getInstance();
        KitPlayerDataManager playerData = KitPlayerDataManager.getInstance();
        if (kitManager == null || playerData == null) return;

        boolean autoGiveStarter = plugin.getConfigFile().getBoolean("kits.auto-give-starter-on-first-join", true);
        if (!autoGiveStarter) return;

        // Only give the starter kit on the very FIRST join ever
        if (player.hasPlayedBefore()) return;

        String starterKitId = plugin.getConfigFile().getString("kits.starter-kit-id", "starter");
        Kit starterKit = kitManager.getKit(starterKitId);
        if (starterKit == null) return;

        // Delay 5 ticks to ensure player is fully loaded
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            // Check permission
            if (!starterKit.getPermission().isEmpty() && !player.hasPermission(starterKit.getPermission())) {
                return;
            }

            // Give the items
            for (org.bukkit.inventory.ItemStack item : starterKit.getItems()) {
                java.util.Map<Integer, org.bukkit.inventory.ItemStack> overflow =
                        player.getInventory().addItem(item.clone());
                for (org.bukkit.inventory.ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }

            // Record the claim so the cooldown starts
            playerData.recordClaim(player.getUniqueId(), starterKit);

            String msg = plugin.getConfigFile().getString("kits.messages.auto-starter",
                    "&#55FF55You received the &#FFD700Starter Kit&#55FF55! Use &f/kit &#55FF55to claim it again later.");
            dev.lorenzz.solum.utils.Messages.send(player, msg);

        }, 5L);
    }
}
