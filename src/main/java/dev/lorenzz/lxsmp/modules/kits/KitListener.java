package dev.lorenzz.lxsmp.modules.kits;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class KitListener implements Listener {

    private final LXSMP plugin;

    public KitListener(LXSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        KitManager kitManager = KitManager.getInstance();
        KitPlayerDataManager playerData = KitPlayerDataManager.getInstance();
        if (kitManager == null || playerData == null) return;

        boolean autoGiveStarter = plugin.getConfigFile().getBoolean("kits.auto-give-starter-on-first-join", true);
        if (!autoGiveStarter) return;

        if (player.hasPlayedBefore()) return;

        String starterKitId = plugin.getConfigFile().getString("kits.starter-kit-id", "starter");
        Kit starterKit = kitManager.getKit(starterKitId);
        if (starterKit == null) return;

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            if (!starterKit.permission().isEmpty() && !player.hasPermission(starterKit.permission())) {
                return;
            }

            for (org.bukkit.inventory.ItemStack item : starterKit.items()) {
                java.util.Map<Integer, org.bukkit.inventory.ItemStack> overflow =
                        player.getInventory().addItem(item.clone());
                for (org.bukkit.inventory.ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }

            playerData.recordClaim(player.getUniqueId(), starterKit);

            String msg = plugin.getConfigFile().getString("kits.messages.auto-starter",
                    "&#55FF55You received the &#FFD700Starter Kit&#55FF55! Use &f/kit &#55FF55to claim it again later.");
            dev.lorenzz.lxsmp.utils.Messages.send(player, msg);

        }, 5L);
    }
}
