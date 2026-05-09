package dev.lorenzz.solum.modules.kits;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.entity.Player;

@CommandAlias("kit|kits")
@Description("Open kit menu or claim a kit")
public class KitCommand extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void kit(Player player, @Optional String kitName) {
        KitManager kitManager = KitManager.getInstance();
        KitPlayerDataManager playerData = KitPlayerDataManager.getInstance();

        if (kitManager == null || playerData == null) {
            Messages.send(player, "&cKit system is not available.");
            return;
        }

        if (kitName == null || kitName.isEmpty()) {
            new KitGUI(player).open();
            return;
        }

        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.not-found",
                    "&#FF4444Kit &f%kit% &#FF4444not found!").replace("%kit%", kitName));
            return;
        }

        claimKit(player, kit, playerData);
    }

    public static void claimKit(Player player, Kit kit, KitPlayerDataManager playerData) {
        Solum plugin = Solum.getInstance();

        if (!kit.getPermission().isEmpty() && !player.hasPermission(kit.getPermission())) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.no-permission",
                    "&#FF4444You don't have permission to claim this kit!"));
            return;
        }

        if (kit.isOneTime() && playerData.hasClaimedOneTime(player.getUniqueId(), kit.getId())) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.already-claimed",
                    "&#FF4444You have already claimed this one-time kit!"));
            return;
        }

        long remaining = playerData.getRemainingCooldown(player.getUniqueId(), kit);
        if (remaining > 0) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.cooldown",
                            "&#FF4444Kit on cooldown! &f%time% &#FF4444remaining.")
                    .replace("%time%", KitPlayerDataManager.formatCooldown(remaining)));
            return;
        }

        for (org.bukkit.inventory.ItemStack item : kit.getItems()) {
            java.util.Map<Integer, org.bukkit.inventory.ItemStack> overflow =
                    player.getInventory().addItem(item.clone());
            for (org.bukkit.inventory.ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        playerData.recordClaim(player.getUniqueId(), kit);

        String msg = plugin.getConfigFile().getString("kits.messages.claimed",
                        "&#55FF55You claimed the %kit% &#55FF55kit!")
                .replace("%kit%", Messages.color(kit.getDisplayName()));
        Messages.send(player, msg);
    }
}

