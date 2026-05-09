package dev.lorenzz.solum.modules.kits;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.not-found"));
            return;
        }

        claimKit(player, kit, playerData);
    }

    public static void claimKit(Player player, Kit kit, KitPlayerDataManager playerData) {
        Solum plugin = Solum.getInstance();

        if (!kit.permission().isEmpty() && !player.hasPermission(kit.permission())) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.no-permission"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        if (kit.oneTime() && playerData.hasClaimedOneTime(player.getUniqueId(), kit.id())) {
            Messages.send(player, plugin.getConfigFile().getString("kits.messages.already-claimed"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
            return;
        }

        boolean bypass = player.hasPermission(KitGUI.COOLDOWN_BYPASS_PERMISSION);
        if (!bypass) {
            long remaining = playerData.getRemainingCooldown(player.getUniqueId(), kit);
            if (remaining > 0) {
                Messages.send(player, plugin.getConfigFile().getString("kits.messages.cooldown")
                        .replace("%time%", KitPlayerDataManager.formatCooldown(remaining)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
                return;
            }
        }

        for (ItemStack item : kit.items()) {
            java.util.Map<Integer, ItemStack> overflow =
                    player.getInventory().addItem(item.clone());
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        playerData.recordClaim(player.getUniqueId(), kit);

        String msg = plugin.getConfigFile().getString("kits.messages.claimed");
        Messages.send(player, msg);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
    }
}

