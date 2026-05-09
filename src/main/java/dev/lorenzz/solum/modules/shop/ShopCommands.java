package dev.lorenzz.solum.modules.shop;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.EconomyManager;
import dev.lorenzz.solum.managers.ShardManager;
import dev.lorenzz.solum.managers.ShopManager;
import dev.lorenzz.solum.managers.ShopManager.ShopCategory;
import dev.lorenzz.solum.managers.ShopManager.ShopCommandItem;
import dev.lorenzz.solum.managers.ShopManager.ShopItem;
import dev.lorenzz.solum.utils.GUI;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @CommandAlias("shop")
    @CommandPermission("solum.shop")
    @Default
    @Description("Open the shop")
    public void shop(Player player) {
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
                    List.of("&#8C8C8C" + (cat.items().size() + cat.commandItems().size()) + " items", "", "&#FFD700&lClick to browse"),
                    p -> openCategoryGUI(p, cat, 0));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        builder.open(player);
    }

    private void openCategoryGUI(Player player, ShopCategory category, int page) {
        int itemsPerPage = 21; // 3 rows of 7 inside border (rows 2-4 of a 6-row GUI)
        List<ShopItem> allItems = category.items();
        List<ShopCommandItem> allCmdItems = category.commandItems();
        int totalEntries = allItems.size() + allCmdItems.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / itemsPerPage));
        int currentPage = Math.min(page, totalPages - 1);
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalEntries);

        int rows = 6;
        GUI.Builder builder = GUI.builder("&#333333&l" + Messages.color(category.displayName()) +
                        " &#8C8C8C(" + (currentPage + 1) + "/" + totalPages + ")", rows)
                .border(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot >= (rows * 9) - 9) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }

            if (i < allItems.size()) {
                // Regular material item
                ShopItem item = allItems.get(i);
                String currencyIcon = item.currency().equalsIgnoreCase("SHARDS") ? "&#4FC3F7Shards " : "&#FFD700$";
                String matName = formatMat(item.material());

                builder.item(slot, item.material(), "&#E0E0E0&l" + matName,
                        List.of(
                                "",
                                "&#8C8C8CBuy: &#55FF55" + currencyIcon + String.format("%.0f", item.buyPrice()),
                                "&#8C8C8CSell: &#FF6B6B" + currencyIcon + String.format("%.0f", item.sellPrice()),
                                "",
                                "&#55FF55Left-click to buy x1",
                                "&#FF4444Right-click to sell all"
                        ),
                        p -> handleBuySell(p, item));
            } else {
                // Command-based custom item (spawner, crate, etc.)
                ShopCommandItem cmdItem = allCmdItems.get(i - allItems.size());
                String currencyIcon = cmdItem.currency().equalsIgnoreCase("SHARDS") ? "&#4FC3F7Shards " : "&#FFD700$";

                List<String> lore = new ArrayList<>();
                lore.add("");
                if (cmdItem.lore() != null) {
                    lore.addAll(cmdItem.lore());
                    lore.add("");
                }
                lore.add("&#8C8C8CBuy: &#55FF55" + currencyIcon + String.format("%.0f", cmdItem.buyPrice()));
                lore.add("");
                lore.add("&#55FF55Click to buy");

                builder.item(slot, cmdItem.displayMaterial(), cmdItem.displayName(), lore,
                        p -> handleCommandBuy(p, cmdItem));
            }
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // Navigation buttons
        if (currentPage > 0) {
            builder.item(45, Material.ARROW, "&#55FF55← Previous Page",
                    List.of("&#8C8C8CPage " + currentPage + "/" + totalPages),
                    p -> openCategoryGUI(p, category, currentPage - 1));
        }

        builder.item(49, Material.BARRIER, "&#8C8C8C← Back to Categories",
                List.of(), p -> openCategoriesGUI(p));

        if (currentPage < totalPages - 1) {
            builder.item(53, Material.ARROW, "&#55FF55Next Page →",
                    List.of("&#8C8C8CPage " + (currentPage + 2) + "/" + totalPages),
                    p -> openCategoryGUI(p, category, currentPage + 1));
        }

        builder.open(player);
    }

    private void handleBuySell(Player player, ShopItem item) {
        // Left-click = buy (default click action)
        handleBuy(player, item);
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

        String currencyName = item.currency().equalsIgnoreCase("SHARDS") ? "Shards " : "$";
        Messages.send(player, "&#55FF55Bought &#FFD700" + formatMat(item.material()) +
                " &#55FF55for &#FFD700" + currencyName + String.format("%.0f", item.buyPrice()));
    }

    private void handleCommandBuy(Player player, ShopCommandItem cmdItem) {
        // Charge the player
        if (cmdItem.currency().equalsIgnoreCase("SHARDS")) {
            ShardManager sm = ShardManager.getInstance();
            if (sm == null) { Messages.send(player, "&cShards not available."); return; }
            if (!sm.removeShards(player.getUniqueId(), (int) cmdItem.buyPrice())) {
                Messages.send(player, "&#FF4444Not enough Shards!");
                return;
            }
        } else {
            EconomyManager eco = EconomyManager.getInstance();
            if (eco == null) { Messages.send(player, "&cEconomy not available."); return; }
            if (!eco.removeBalance(player.getUniqueId(), cmdItem.buyPrice())) {
                Messages.send(player, "&#FF4444Not enough money!");
                return;
            }
        }

        // Execute the console command with {player} replaced
        String command = cmdItem.command().replace("{player}", player.getName());
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));

        String currencyName = cmdItem.currency().equalsIgnoreCase("SHARDS") ? "Shards " : "$";
        Messages.send(player, "&#55FF55Bought &#FFD700" + Messages.color(cmdItem.displayName()) +
                " &#55FF55for &#FFD700" + currencyName + String.format("%.0f", cmdItem.buyPrice()));
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
