package dev.lorenzz.hirustuffs.modules.worth;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Named;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.common.annotations.Optional;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.WorthManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class WorthCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "worth", description = "Check item worth", async = false)
    public void worth(@Sender Player player, @Optional @Named("item") String itemName) {
        WorthManager wm = WorthManager.getInstance();
        if (wm == null) { Messages.send(player, "&cWorth system not available."); return; }

        if (itemName != null && !itemName.isEmpty()) {
            Material mat = Material.matchMaterial(itemName.toUpperCase());
            if (mat == null) { Messages.send(player, "&#FF4444Unknown item: &f" + itemName); return; }
            double price = wm.getPrice(mat);
            if (price <= 0) { Messages.send(player, "&#FF4444This item has no sell value."); return; }
            Messages.send(player, "&#E0E0E0Worth of &#FFD700" + formatMat(mat) + "&#8C8C8C: &#55FF55$" + String.format("%.2f", price));
            return;
        }

        // Check item in hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) { Messages.send(player, "&#8C8C8CHold an item or use &#E0E0E0/worth <item>"); return; }

        double price = wm.getPrice(hand.getType());
        if (price <= 0) { Messages.send(player, "&#FF4444This item has no sell value."); return; }
        double total = price * hand.getAmount();
        Messages.send(player, "&#E0E0E0Worth of &#FFD700" + formatMat(hand.getType()) +
                " &#8C8C8Cx" + hand.getAmount() + "&#8C8C8C: &#55FF55$" + String.format("%.2f", total) +
                " &#8C8C8C(&#55FF55$" + String.format("%.2f", price) + " &#8C8C8Ceach)");
    }

    @Command(name = "prices", aliases = {"worthlist"}, description = "Open prices menu", async = false)
    public void prices(@Sender Player player) {
        openPricesGUI(player, 0);
    }

    private void openPricesGUI(Player player, int page) {
        WorthManager wm = WorthManager.getInstance();
        if (wm == null) { Messages.send(player, "&cWorth system not available."); return; }

        List<Map.Entry<Material, Double>> sorted = wm.getSortedPrices();
        if (sorted.isEmpty()) { Messages.send(player, "&#8C8C8CNo items have prices set."); return; }

        int itemsPerPage = 21;
        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / itemsPerPage));
        int currentPage = Math.min(page, totalPages - 1);
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, sorted.size());

        int rows = 6;
        GUI.Builder builder = GUI.builder("&#333333&lItem Prices &#8C8C8C(" + (currentPage + 1) + "/" + totalPages + ")", rows)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            if (slot >= (rows * 9) - 9) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }

            Map.Entry<Material, Double> entry = sorted.get(i);
            builder.item(slot, entry.getKey(), "&#FFD700" + formatMat(entry.getKey()),
                    List.of("&#8C8C8CSell Price: &#55FF55$" + String.format("%.2f", entry.getValue())));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // Navigation
        if (currentPage > 0) {
            builder.item(45, Material.ARROW, "&#55FF55← Previous Page",
                    List.of("&#8C8C8CPage " + currentPage + "/" + totalPages),
                    p -> openPricesGUI(p, currentPage - 1));
        }

        // Page info
        builder.item(49, Material.BOOK, "&#E0E0E0&lPage " + (currentPage + 1) + "/" + totalPages,
                List.of("&#8C8C8CTotal items: &#FFD700" + sorted.size()),
                null);

        if (currentPage < totalPages - 1) {
            builder.item(53, Material.ARROW, "&#55FF55Next Page →",
                    List.of("&#8C8C8CPage " + (currentPage + 2) + "/" + totalPages),
                    p -> openPricesGUI(p, currentPage + 1));
        }

        builder.open(player);
    }

    @Command(name = "setworth", description = "Set item worth", async = false)
    @Permission("hiru.admin")
    public void setWorth(@Sender Player sender, @Named("item") String itemName, @Named("price") double price) {
        WorthManager wm = WorthManager.getInstance();
        Material mat = Material.matchMaterial(itemName.toUpperCase());
        if (mat == null) { Messages.send(sender, "&#FF4444Unknown item: &f" + itemName); return; }
        wm.setPrice(mat, price);
        Messages.send(sender, "&#55FF55Set price of &#FFD700" + formatMat(mat) + " &#55FF55to &#FFD700$" + String.format("%.2f", price));
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
