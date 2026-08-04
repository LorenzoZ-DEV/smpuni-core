package dev.lorenzz.lxsmp.modules.sell;

import dev.lorenzz.lxsmp.managers.EconomyManager;
import dev.lorenzz.lxsmp.managers.WorthManager;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Listener for the raw sell GUI. Players can place items in the open slots
 * (non-border, non-button slots) and click "Sell All" to sell them.
 */
public class SellMenuListener implements Listener {

    /** Title used to identify sell menus. Must match what SellCommands uses. */
    public static final String SELL_MENU_TITLE = "Sell Items";

    /** Slots that are interactive (players can place items here) — rows 2-4 excluding borders */
    private static final Set<Integer> OPEN_SLOTS = new HashSet<>();

    static {
        for (int i = 10; i < 44; i++) {
            if (i % 9 != 0 && i % 9 != 8) {
                OPEN_SLOTS.add(i);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || topInv.getSize() != 54) return;

        String title = event.getView().getTitle();
        if (!title.contains(SELL_MENU_TITLE)) return;

        int rawSlot = event.getRawSlot();

        // Click in player inventory (bottom) — allow freely for shift-click handling
        if (rawSlot >= topInv.getSize()) {
            // Shift-click from player inventory INTO the sell area
            if (event.isShiftClick()) {
                event.setCancelled(true);
                // Manually move the item to the first open slot
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR) return;

                for (int slot : OPEN_SLOTS) {
                    ItemStack existing = topInv.getItem(slot);
                    if (existing == null || existing.getType() == Material.AIR) {
                        topInv.setItem(slot, clicked.clone());
                        event.setCurrentItem(null);
                        return;
                    }
                    // Stack with existing same-type items
                    if (existing.isSimilar(clicked)) {
                        int canFit = existing.getMaxStackSize() - existing.getAmount();
                        if (canFit > 0) {
                            int transfer = Math.min(canFit, clicked.getAmount());
                            existing.setAmount(existing.getAmount() + transfer);
                            clicked.setAmount(clicked.getAmount() - transfer);
                            if (clicked.getAmount() <= 0) {
                                event.setCurrentItem(null);
                                return;
                            }
                        }
                    }
                }
                // If we got here, there was no room for remaining items
            }
            return; // Normal clicks in player inventory are allowed
        }

        // Click in top inventory
        if (OPEN_SLOTS.contains(rawSlot)) {
            // Allow placing/taking items in the sell area
            return;
        }

        // Button slots
        event.setCancelled(true);

        if (rawSlot == 49) {
            // Sell All button
            sellItemsInMenu(player, topInv);
        } else if (rawSlot == 45) {
            // Cancel button — close returns items via onClose
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || topInv.getSize() != 54) return;

        String title = event.getView().getTitle();
        if (!title.contains(SELL_MENU_TITLE)) return;

        // Only allow dragging into open slots
        for (int slot : event.getRawSlots()) {
            if (slot < topInv.getSize() && !OPEN_SLOTS.contains(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null || topInv.getSize() != 54) return;

        String title = event.getView().getTitle();
        if (!title.contains(SELL_MENU_TITLE)) return;

        // Return all items in open slots to the player
        for (int slot : OPEN_SLOTS) {
            ItemStack item = topInv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                player.getInventory().addItem(item).values().forEach(leftover ->
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover));
                topInv.setItem(slot, null);
            }
        }
    }

    private void sellItemsInMenu(Player player, Inventory inventory) {
        WorthManager wm = WorthManager.getInstance();
        EconomyManager eco = EconomyManager.getInstance();
        if (wm == null || eco == null) {
            Messages.send(player, "&cSell system not available.");
            return;
        }

        double total = 0;
        int itemsSold = 0;
        List<String> soldItems = new ArrayList<>();

        for (int slot : OPEN_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            double price = wm.getPrice(item.getType());
            if (price <= 0) continue;

            double itemTotal = price * item.getAmount();
            total += itemTotal;
            itemsSold += item.getAmount();
            soldItems.add("&#8C8C8C" + item.getAmount() + "x " + formatMat(item.getType()) + " &#55FF55$" + Messages.formatNumber(itemTotal));
            inventory.setItem(slot, null);
        }

        if (itemsSold == 0) {
            Messages.send(player, "&#8C8C8CNo sellable items found.");
            return;
        }

        eco.addBalance(player.getUniqueId(), total);

        Messages.send(player, "&#3D3D3D━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Messages.send(player, " &#55FF55&lITEMS SOLD");
        for (String line : soldItems) {
            Messages.sendRaw(player, " " + line);
        }
        Messages.send(player, "");
        Messages.send(player, " &#E0E0E0Total: &#FFD700&l$" + Messages.formatNumber(total));
        Messages.send(player, "&#3D3D3D━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private String formatMat(Material material) {
        String name = material.name().replace('_', ' ');
        StringBuilder result = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase()).append(' ');
            }
        }
        return result.toString().trim();
    }
}
