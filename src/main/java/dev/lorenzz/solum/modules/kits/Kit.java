package dev.lorenzz.solum.modules.kits;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Data object representing a single kit.
 */
public class Kit {

    private final String id;
    private final String displayName;
    private final int cooldown;       // seconds, 0 = no cooldown
    private final boolean oneTime;
    private final String permission;  // empty = no permission
    private final Material icon;
    private final List<String> lore;
    private final List<ItemStack> items;

    public Kit(String id, String displayName, int cooldown, boolean oneTime,
               String permission, Material icon, List<String> lore, List<ItemStack> items) {
        this.id = id;
        this.displayName = displayName;
        this.cooldown = cooldown;
        this.oneTime = oneTime;
        this.permission = permission;
        this.icon = icon;
        this.lore = lore;
        this.items = items;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getCooldown() { return cooldown; }
    public boolean isOneTime() { return oneTime; }
    public String getPermission() { return permission; }
    public Material getIcon() { return icon; }
    public List<String> getLore() { return lore; }
    public List<ItemStack> getItems() { return items; }
}
