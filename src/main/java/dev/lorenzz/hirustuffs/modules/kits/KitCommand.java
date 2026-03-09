package dev.lorenzz.hirustuffs.modules.kits;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;

public class KitCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "kit", aliases = {"kits"}, description = "Open kit menu or claim a kit", async = false)
    public void kit(@Sender Player player, @Optional @Named("kit") String kitName) {
        KitManager kitManager = KitManager.getInstance();
        KitPlayerDataManager playerData = KitPlayerDataManager.getInstance();

        if (kitManager == null || playerData == null) {
            Messages.send(player, "&cKit system is not available.");
            return;
        }

        // If no kit specified, open the GUI
        if (kitName == null || kitName.isEmpty()) {
            new KitGUI(player).open();
            return;
        }

        // Claim a specific kit by name
        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.not-found",
                    "&#FF4444Kit &f%kit% &#FF4444not found!").replace("%kit%", kitName));
            return;
        }

        claimKit(player, kit, playerData);
    }

    public static void claimKit(Player player, Kit kit, KitPlayerDataManager playerData) {
        Hiru plugin = Hiru.getInstance();

        // Permission check
        if (!kit.getPermission().isEmpty() && !player.hasPermission(kit.getPermission())) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.no-permission",
                    "&#FF4444You don't have permission to claim this kit!"));
            return;
        }

        // One-time check
        if (kit.isOneTime() && playerData.hasClaimedOneTime(player.getUniqueId(), kit.getId())) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.already-claimed",
                    "&#FF4444You have already claimed this one-time kit!"));
            return;
        }

        // Cooldown check
        long remaining = playerData.getRemainingCooldown(player.getUniqueId(), kit);
        if (remaining > 0) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.cooldown",
                            "&#FF4444Kit on cooldown! &f%time% &#FF4444remaining.")
                    .replace("%time%", KitPlayerDataManager.formatCooldown(remaining)));
            return;
        }

        // Give items — overflow drops on the ground
        for (org.bukkit.inventory.ItemStack item : kit.getItems()) {
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> overflow =
                    player.getInventory().addItem(item.clone());
            for (org.bukkit.inventory.ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        // Record the claim
        playerData.recordClaim(player.getUniqueId(), kit);

        String msg = plugin.getConfigFile().getString("kits.messages.claimed",
                "&#55FF55You claimed the %kit% &#55FF55kit!")
                .replace("%kit%", Messages.color(kit.getDisplayName()));
        Messages.send(player, msg);
    }
}
