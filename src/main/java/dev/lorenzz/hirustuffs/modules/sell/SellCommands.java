package dev.lorenzz.hirustuffs.modules.sell;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.WorthManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.j4c0b3y.api.menu.Menu;
import net.j4c0b3y.api.menu.MenuSize;
import net.j4c0b3y.api.menu.button.impl.PlaceholderButton;
import net.j4c0b3y.api.menu.button.impl.SimpleButton;
import net.j4c0b3y.api.menu.layer.impl.BackgroundLayer;
import net.j4c0b3y.api.menu.layer.impl.ForegroundLayer;
import net.j4c0b3y.api.menu.utils.Item;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
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

        Menu menu = new Menu("&#333333&lSell Items", MenuSize.of(6), player) {
            @Override
            public void setup(BackgroundLayer bg, ForegroundLayer fg) {
                // Border
                bg.border(new PlaceholderButton(XMaterial.BLACK_STAINED_GLASS_PANE));

                // Fill row 5 (slots 45-53) with glass
                for (int i = 45; i < 54; i++) {
                    bg.set(i, new PlaceholderButton(XMaterial.BLACK_STAINED_GLASS_PANE));
                }

                // Info item
                fg.set(4, new SimpleButton(
                        new Item(XMaterial.HOPPER)
                                .setName("&#FFD700&lPlace items below to sell")
                                .setLore(List.of("&#8C8C8CDrop sellable items in the empty slots", "&#8C8C8CThen click the Sell button"))
                                .build()
                ));

                // Sell All button
                fg.set(49, new SimpleButton(
                        new Item(XMaterial.LIME_STAINED_GLASS_PANE)
                                .setName("&#55FF55&lSELL ALL")
                                .setLore(List.of("&#8C8C8CClick to sell all items above"))
                                .build(),
                        click -> {
                            Player p = click.getMenu().getPlayer();
                            sellItemsInMenu(p, click.getMenu());
                        }
                ));

                // Cancel button
                fg.set(45, new SimpleButton(
                        new Item(XMaterial.RED_STAINED_GLASS_PANE)
                                .setName("&#FF4444&lCANCEL")
                                .setLore(List.of("&#8C8C8CClose and return items"))
                                .build(),
                        click -> click.getMenu().getPlayer().closeInventory()
                ));

                // Worth info button
                fg.set(53, new SimpleButton(
                        new Item(XMaterial.BOOK)
                                .setName("&#4FC3F7&lPrices")
                                .setLore(List.of("&#8C8C8CUse /prices to see all item values"))
                                .build()
                ));
            }

            @Override
            public void onClose() {
                // Return items that weren't sold
                Player p = getPlayer();
                for (int i = 10; i < 44; i++) {
                    if (i % 9 == 0 || i % 9 == 8) continue;
                    ItemStack item = getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        p.getInventory().addItem(item).values().forEach(leftover ->
                                p.getWorld().dropItemNaturally(p.getLocation(), leftover));
                    }
                }
            }
        };
        menu.open();
    }

    private void sellItemsInMenu(Player player, Menu menu) {
        WorthManager wm = WorthManager.getInstance();
        EconomyManager eco = EconomyManager.getInstance();
        if (wm == null || eco == null) return;

        double total = 0;
        int itemsSold = 0;
        List<String> soldItems = new ArrayList<>();

        for (int i = 10; i < 44; i++) {
            if (i % 9 == 0 || i % 9 == 8) continue;
            ItemStack item = menu.getInventory().getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            double price = wm.getPrice(item.getType());
            if (price <= 0) continue;

            double itemTotal = price * item.getAmount();
            total += itemTotal;
            itemsSold += item.getAmount();
            soldItems.add("&#8C8C8C" + item.getAmount() + "x " + formatMat(item.getType()) + " &#55FF55$" + Messages.formatNumber(itemTotal));
            menu.getInventory().setItem(i, null);
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
