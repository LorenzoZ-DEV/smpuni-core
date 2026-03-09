package dev.lorenzz.hirustuffs.modules.kits;

import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles first-join auto-kit granting (starter kit).
 */
public class KitListener implements Listener {

    private final Hiru plugin;

    public KitListener(Hiru plugin) {
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

        String starterKitId = plugin.getConfigFile().getString("kits.starter-kit-id", "starter");
        Kit starterKit = kitManager.getKit(starterKitId);
        if (starterKit == null) return;

        // Only give if not already claimed (one-time)
        if (playerData.hasClaimedOneTime(player.getUniqueId(), starterKit.getId())) {
            return;
        }

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

            playerData.recordClaim(player.getUniqueId(), starterKit);

            String msg = plugin.getConfigFile().getString("kits.messages.auto-starter",
                    "&#55FF55You received the &#FFD700Starter Kit&#55FF55! Use &f/kit &#55FF55to view all kits.");
            dev.lorenzz.hirustuffs.utils.Messages.send(player, msg);

        }, 5L);
    }
}
