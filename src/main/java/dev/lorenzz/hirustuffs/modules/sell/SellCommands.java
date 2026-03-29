package dev.lorenzz.hirustuffs.modules.sell;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.WorthManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SellCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "sell", description = "Open sell menu", async = false)
    @Permission("hiru.sell")
    public void sell(@Sender Player player) {
        openSellMenu(player);
    }

    @Command(name = "sellhand", description = "Sell item in hand", async = false)
    @Permission("hiru.sell")
    public void sellHand(@Sender Player player) {
        WorthManager wm = WorthManager.getInstance();
        EconomyManager eco = EconomyManager.getInstance();
        if (wm == null || eco == null) { Messages.send(player, "&cSell system not available."); return; }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) { Messages.send(player, "&#FF4444You are not holding anything!"); return; }

        double price = wm.getPrice(hand.getType());
        if (price <= 0) { Messages.send(player, "&#FF4444This item cannot be sold!"); return; }

        double total = price * hand.getAmount();
        eco.addBalance(player.getUniqueId(), total);
        player.getInventory().setItemInMainHand(null);

        Messages.send(player, "&#55FF55Sold &#FFD700" + hand.getAmount() + "x " + formatMat(hand.getType()) +
                " &#55FF55for &#FFD700$" + Messages.formatNumber(total));
    }

    @Command(name = "sellall", description = "Sell all sellable items", async = false)
    @Permission("hiru.sell")
    public void sellAll(@Sender Player player) {
        WorthManager wm = WorthManager.getInstance();
        EconomyManager eco = EconomyManager.getInstance();
        if (wm == null || eco == null) { Messages.send(player, "&cSell system not available."); return; }

        double total = 0;
        int itemsSold = 0;

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            double price = wm.getPrice(item.getType());
            if (price <= 0) continue;

            total += price * item.getAmount();
            itemsSold += item.getAmount();
            player.getInventory().setItem(i, null);
        }

        if (itemsSold == 0) {
            Messages.send(player, "&#8C8C8CNo sellable items in your inventory.");
            return;
        }

        eco.addBalance(player.getUniqueId(), total);
        Messages.send(player, "&#55FF55Sold &#FFD700" + itemsSold + " items &#55FF55for &#FFD700$" + Messages.formatNumber(total));
    }

    private void openSellMenu(Player player) {
        WorthManager wm = WorthManager.getInstance();
        EconomyManager eco = EconomyManager.getInstance();
        if (wm == null || eco == null) { Messages.send(player, "&cSell system not available."); return; }

        String title = Messages.color("&#333333&l" + SellMenuListener.SELL_MENU_TITLE);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Border — fill row 0 (0-8), row 5 (45-53), and columns 0 & 8 of rows 1-4
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, border);
            inv.setItem(row * 9 + 8, border);
        }

        // Info item (slot 4)
        inv.setItem(4, createGuiItem(Material.HOPPER,
                Messages.color("&#FFD700&lDrop items to sell"),
                Messages.color("&#8C8C8CPlace sellable items in the empty slots"),
                Messages.color("&#8C8C8CThen click the Sell button")));

        // Sell All button (slot 49)
        inv.setItem(49, createGuiItem(Material.LIME_STAINED_GLASS_PANE,
                Messages.color("&#55FF55&lSELL ALL"),
                Messages.color("&#8C8C8CClick to sell all items above")));

        // Cancel button (slot 45)
        inv.setItem(45, createGuiItem(Material.RED_STAINED_GLASS_PANE,
                Messages.color("&#FF4444&lCANCEL"),
                Messages.color("&#8C8C8CClose and return items")));

        // Prices info (slot 53)
        inv.setItem(53, createGuiItem(Material.BOOK,
                Messages.color("&#4FC3F7&lPrices"),
                Messages.color("&#8C8C8CUse /prices to see all item values")));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(List.of(lore));
            item.setItemMeta(meta);
        }
        return item;
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
