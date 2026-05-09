package dev.lorenzz.solum.modules.kits;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.GUI;
import dev.lorenzz.solum.utils.Messages;
import net.j4c0b3y.api.menu.utils.Item;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GUI for /kit — shows all available kits as icons.
 */
public class KitGUI {

    private final Player player;

    public KitGUI(Player player) {
        this.player = player;
    }

    public void open() {
        KitManager kitManager = KitManager.getInstance();
        KitPlayerDataManager playerData = KitPlayerDataManager.getInstance();
        if (kitManager == null || playerData == null) return;

        Collection<Kit> kits = kitManager.getKits();

        // Calculate rows: min 3, max 6
        int itemCount = kits.size();
        int rows = Math.min(6, Math.max(3, (int) Math.ceil((itemCount + 9) / 9.0) + 1));

        String title = Solum.getInstance().getConfigFile().getString("kits.gui.title", "&#FFD700&l⚡ Kits");

        GUI.Builder builder = GUI.builder(Messages.color(title), rows)
                .border(Material.BLACK_STAINED_GLASS_PANE)
                .fill(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10; // Start inside border
        int maxSlot = (rows * 9) - 10; // Last slot inside border

        for (Kit kit : kits) {
            if (slot >= maxSlot) break;

            // Skip border slots
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }

            ItemStack icon = buildKitIcon(kit, playerData);
            final Kit finalKit = kit;

            builder.item(slot, icon, p -> KitCommand.claimKit(p, finalKit, playerData));
            slot++;
        }

        builder.open(player);
    }

    private ItemStack buildKitIcon(Kit kit, KitPlayerDataManager playerData) {
        ItemStack icon = new ItemStack(kit.getIcon());
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;

        meta.setDisplayName(Messages.color(kit.getDisplayName()));

        List<String> lore = new ArrayList<>();

        // Kit description lore from config
        for (String line : kit.getLore()) {
            lore.add(Messages.color(line));
        }

        lore.add("");

        // Permission check
        boolean hasPermission = kit.getPermission().isEmpty() || player.hasPermission(kit.getPermission());

        if (!hasPermission) {
            lore.add(Messages.color("&#FF4444✖ No Permission"));
        } else if (kit.isOneTime() && playerData.hasClaimedOneTime(player.getUniqueId(), kit.getId())) {
            lore.add(Messages.color("&#FF8B76✔ Already Claimed"));
        } else {
            long remaining = playerData.getRemainingCooldown(player.getUniqueId(), kit);
            if (remaining > 0) {
                lore.add(Messages.color("&#FFD700⏱ Cooldown: &f" + KitPlayerDataManager.formatCooldown(remaining)));
            } else {
                lore.add(Messages.color("&#55FF55✔ Available"));
            }
        }

        // Items preview
        lore.add("");
        lore.add(Messages.color("&7Items: &f" + kit.getItems().size()));

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }
}
