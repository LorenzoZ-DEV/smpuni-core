package dev.lorenzz.hirustuffs.modules.shop;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.managers.ShopManager;
import dev.lorenzz.hirustuffs.managers.ShopManager.ShopCategory;
import dev.lorenzz.hirustuffs.managers.ShopManager.ShopItem;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ShopCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "shop", description = "Open the shop", async = false)
    @Permission("hiru.shop")
    public void shop(@Sender Player player) {
        openCategoriesGUI(player);
    }

    private void openCategoriesGUI(Player player) {
        ShopManager sm = ShopManager.getInstance();
        if (sm == null) { Messages.send(player, "&cShop not available."); return; }

        Map<String, ShopCategory> categories = sm.getCategories();
        int rows = Math.min(Math.max((categories.size() / 7) + 3, 3), 6);
        GUI.Builder builder = GUI.builder("&#333333&lShop", rows).border(Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (Map.Entry<String, ShopCategory> entry : categories.entrySet()) {
            ShopCategory cat = entry.getValue();
            builder.item(slot, cat.icon(), cat.displayName(),
                    List.of("&#8C8C8C" + cat.items().size() + " items", "", "&#FFD700&lClick to browse"),
                    p -> openCategoryGUI(p, cat));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        builder.open(player);
    }

    private void openCategoryGUI(Player player, ShopCategory category) {
        int rows = Math.min(Math.max((category.items().size() / 7) + 3, 3), 6);
        GUI.Builder builder = GUI.builder("&#333333&l" + Messages.color(category.displayName()), rows)
                .border(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        int maxSlots = (rows * 9) - 9;
        for (ShopItem item : category.items()) {
            if (slot >= maxSlots) break;
            String currencyIcon = item.currency().equalsIgnoreCase("SHARDS") ? "&#4FC3F7Shards" : "&#FFD700$";
            String matName = formatMat(item.material());

            builder.item(slot, item.material(), "&#E0E0E0&l" + matName,
                    List.of(
                            "",
                            "&#8C8C8CBuy: &#55FF55" + currencyIcon + String.format("%.0f", item.buyPrice()),
                            "&#8C8C8CSell: &#FF6B6B" + currencyIcon + String.format("%.0f", item.sellPrice()),
                            "",
                            "&#55FF55Left-click to buy",
                            "&#FF4444Right-click to sell"
                    ),
                    p -> handleBuy(p, item));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        builder.item((rows * 9) - 5, Material.ARROW, "&#8C8C8C← Back", List.of(),
                p -> openCategoriesGUI(p));

        builder.open(player);
    }

    private void handleBuy(Player player, ShopItem item) {
        if (item.currency().equalsIgnoreCase("SHARDS")) {
            ShardManager sm = ShardManager.getInstance();
            if (sm == null) { Messages.send(player, "&cShards not available."); return; }
            if (!sm.removeShards(player.getUniqueId(), (int) item.buyPrice())) {
                Messages.send(player, "&#FF4444Not enough Shards!");
                return;
            }
        } else {
            EconomyManager eco = EconomyManager.getInstance();
            if (eco == null) { Messages.send(player, "&cEconomy not available."); return; }
            if (!eco.removeBalance(player.getUniqueId(), item.buyPrice())) {
                Messages.send(player, "&#FF4444Not enough money!");
                return;
            }
        }

        player.getInventory().addItem(new ItemStack(item.material(), 1)).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));

        String currencyName = item.currency().equalsIgnoreCase("SHARDS") ? "Shards" : "$";
        Messages.send(player, "&#55FF55Bought &#FFD700" + formatMat(item.material()) +
                " &#55FF55for &#FFD700" + currencyName + String.format("%.0f", item.buyPrice()));
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
