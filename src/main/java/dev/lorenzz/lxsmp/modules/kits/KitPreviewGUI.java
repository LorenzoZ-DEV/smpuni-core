package dev.lorenzz.lxsmp.modules.kits;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

public class KitPreviewGUI implements InventoryHolder, Listener {

    private static final int BACK_SLOT_OFFSET = 1;

    private final Player player;
    private final Kit kit;
    private final Inventory inventory;
    private final int backSlot;

    public KitPreviewGUI(Player player, Kit kit) {
        this.player = player;
        this.kit = kit;

        int itemCount = kit.items().size();
        int contentRows = Math.max(1, Math.min(5, (int) Math.ceil(itemCount / 9.0)));
        int rows = contentRows + 1;
        int size = rows * 9;

        String title = Messages.color("&8Preview › " + kit.displayName());
        this.inventory = Bukkit.createInventory(this, size, title);
        this.backSlot = size - BACK_SLOT_OFFSET;

        int slot = 0;
        for (ItemStack item : kit.items()) {
            if (slot >= size - 9) break;
            inventory.setItem(slot++, item.clone());
        }

        inventory.setItem(backSlot, buildBackButton());

        Bukkit.getPluginManager().registerEvents(this, LXSMP.getInstance());
    }

    private ItemStack buildBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&c« Back to kits"));
            meta.setLore(loreOf(Messages.color("&7Click to return")));
            back.setItemMeta(meta);
        }
        return back;
    }

    private List<String> loreOf(String line) {
        return Collections.singletonList(line);
    }

    public void open() {
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!this.equals(event.getInventory().getHolder())) return;

        event.setCancelled(true);

        if (event.getRawSlot() == backSlot) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
                    new KitGUI(clicker).open();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (this.equals(event.getInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (this.equals(event.getInventory().getHolder())) {
            HandlerList.unregisterAll(this);
        }
    }
}

