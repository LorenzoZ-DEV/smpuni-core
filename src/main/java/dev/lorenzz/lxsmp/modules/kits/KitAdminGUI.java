package dev.lorenzz.lxsmp.modules.kits;

import dev.lorenzz.lxsmp.utils.GUI;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class KitAdminGUI {

    private final Player player;

    public KitAdminGUI(Player player) {
        this.player = player;
    }

    public void open() {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) return;

        Collection<Kit> kits = kitManager.getKits();
        int itemCount = kits.size();
        int rows = Math.min(6, Math.max(3, (int) Math.ceil((itemCount + 9) / 9.0) + 1));

        GUI.Builder builder = GUI.builder(Messages.color("&#FF4444&l⚙ Kit Admin"), rows)
                .border(Material.RED_STAINED_GLASS_PANE)
                .fill(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        int maxSlot = (rows * 9) - 10;

        for (Kit kit : kits) {
            if (slot >= maxSlot) break;
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }

            ItemStack icon = buildAdminKitIcon(kit);
            final String kitId = kit.id();

            builder.item(slot, icon, p -> {
                openKitPreview(p, kitId);
            });
            slot++;
        }

        int bottomCenter = (rows - 1) * 9 + 4;
        ItemStack createBtn = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = createBtn.getItemMeta();
        if (createMeta != null) {
            createMeta.setDisplayName(Messages.color("&#55FF55&l+ Create New Kit"));
            createMeta.setLore(Arrays.asList(
                    Messages.color("&7Use &f/kitadmin create <name>"),
                    Messages.color("&7with items in your inventory.")
            ));
            createBtn.setItemMeta(createMeta);
        }
        builder.item(bottomCenter, createBtn);

        int reloadSlot = (rows - 1) * 9 + 6;
        ItemStack reloadBtn = new ItemStack(Material.REDSTONE);
        ItemMeta reloadMeta = reloadBtn.getItemMeta();
        if (reloadMeta != null) {
            reloadMeta.setDisplayName(Messages.color("&#FFD700&l↻ Reload Kits"));
            reloadMeta.setLore(Collections.singletonList(
                    Messages.color("&7Click to reload all kits from file.")
            ));
            reloadBtn.setItemMeta(reloadMeta);
        }
        builder.item(reloadSlot, reloadBtn, p -> {
            KitManager.getInstance().reload();
            Messages.send(p, "&#55FF55Kits reloaded!");
            p.closeInventory();
            new KitAdminGUI(p).open();
        });

        builder.open(player);
    }

    private ItemStack buildAdminKitIcon(Kit kit) {
        ItemStack icon = new ItemStack(kit.icon());
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;

        meta.setDisplayName(Messages.color(kit.displayName()));

        List<String> lore = new ArrayList<>();
        lore.add(Messages.color("&7ID: &f" + kit.id()));
        lore.add(Messages.color("&7Permission: &f" + (kit.permission().isEmpty() ? "none" : kit.permission())));
        lore.add(Messages.color("&7Cooldown: &f" + (kit.cooldown() <= 0 ? "none" : kit.cooldown() + "s")));
        lore.add(Messages.color("&7One-time: &f" + (kit.oneTime() ? "yes" : "no")));
        lore.add(Messages.color("&7Items: &f" + kit.items().size()));
        lore.add("");
        lore.add(Messages.color("&#72FF97&lClick &7to preview contents"));
        lore.add(Messages.color("&#FF8B76&l/kitadmin delete " + kit.id()));

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private void openKitPreview(Player player, String kitId) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) return;

        Kit kit = kitManager.getKit(kitId);
        if (kit == null) return;

        List<ItemStack> items = kit.items();
        int rows = Math.min(6, Math.max(1, (int) Math.ceil(items.size() / 9.0)));

        GUI.Builder builder = GUI.builder(Messages.color("&8Preview: " + kit.displayName()), rows);

        for (int i = 0; i < items.size() && i < rows * 9; i++) {
            builder.item(i, items.get(i).clone());
        }

        builder.open(player);
    }
}
