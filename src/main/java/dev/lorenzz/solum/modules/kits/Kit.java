package dev.lorenzz.solum.modules.kits;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Data object representing a single kit.
 *
 * @param cooldown   seconds, 0 = no cooldown
 * @param permission empty = no permission
 */
public record Kit(String id, String displayName, int cooldown, boolean oneTime, String permission, Material icon,
                  List<String> lore, List<ItemStack> items) {

}
