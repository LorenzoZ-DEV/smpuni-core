package dev.lorenzz.hirustuffs.utils;

import com.cryptomorin.xseries.XMaterial;
import net.j4c0b3y.api.menu.Menu;
import net.j4c0b3y.api.menu.MenuSize;
import net.j4c0b3y.api.menu.button.impl.PlaceholderButton;
import net.j4c0b3y.api.menu.button.impl.SimpleButton;
import net.j4c0b3y.api.menu.layer.impl.BackgroundLayer;
import net.j4c0b3y.api.menu.layer.impl.ForegroundLayer;
import net.j4c0b3y.api.menu.utils.Item;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

/**
 * GUI builder wrapper around MenuAPI (net.j4c0b3y).
 * Provides a simple builder API while using MenuAPI under the hood.
 */
public class GUI {

    public static Builder builder(String title, int rows) {
        return new Builder(title, rows);
    }

    /**
     * Legacy compat — no-op since MenuAPI handles click/close events internally.
     */
    public static void handleClick(org.bukkit.event.inventory.InventoryClickEvent event) { }
    public static void handleClose(Player player) { }

    private static XMaterial toXMaterial(Material material) {
        return XMaterial.matchXMaterial(material);
    }

    public static class Builder {
        private final String title;
        private final int rows;
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        private Material borderMaterial;
        private Material fillMaterial;

        private Builder(String title, int rows) {
            this.title = title;
            this.rows = Math.min(Math.max(rows, 1), 6);
        }

        public Builder item(int slot, ItemStack item) {
            items.put(slot, item);
            return this;
        }

        public Builder item(int slot, ItemStack item, Consumer<Player> onClick) {
            items.put(slot, item);
            if (onClick != null) actions.put(slot, onClick);
            return this;
        }

        public Builder item(int slot, Material material, String name, List<String> lore) {
            Item item = new Item(toXMaterial(material)).setName(name);
            if (lore != null && !lore.isEmpty()) {
                item.setLore(lore);
            }
            items.put(slot, item.build());
            return this;
        }

        public Builder item(int slot, Material material, String name, List<String> lore, Consumer<Player> onClick) {
            item(slot, material, name, lore);
            if (onClick != null) actions.put(slot, onClick);
            return this;
        }

        public Builder border(Material material) {
            this.borderMaterial = material;
            return this;
        }

        public Builder fill(Material material) {
            this.fillMaterial = material;
            return this;
        }

        public void open(Player player) {
            final String menuTitle = title;
            final int menuRows = rows;
            final Map<Integer, ItemStack> menuItems = new HashMap<>(items);
            final Map<Integer, Consumer<Player>> menuActions = new HashMap<>(actions);
            final Material border = borderMaterial;
            final Material filler = fillMaterial;

            Menu menu = new Menu(menuTitle, MenuSize.of(menuRows), player) {
                @Override
                public void setup(BackgroundLayer bg, ForegroundLayer fg) {
                    if (border != null) {
                        bg.border(new PlaceholderButton(toXMaterial(border)));
                    }

                    if (filler != null) {
                        bg.fill(new PlaceholderButton(toXMaterial(filler)));
                    }

                    for (Map.Entry<Integer, ItemStack> entry : menuItems.entrySet()) {
                        int slot = entry.getKey();
                        ItemStack itemStack = entry.getValue();
                        Consumer<Player> action = menuActions.get(slot);

                        if (action != null) {
                            fg.set(slot, new SimpleButton(itemStack, click ->
                                    action.accept(click.getMenu().getPlayer())
                            ));
                        } else {
                            fg.set(slot, new SimpleButton(itemStack));
                        }
                    }
                }
            };
            menu.open();
        }
    }
}
