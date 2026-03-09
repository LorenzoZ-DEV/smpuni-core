package dev.lorenzz.hirustuffs.modules.kits;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KitAdminCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "kitadmin", description = "Kit administration panel", async = false)
    @Permission("hiru.admin")
    public void kitadmin(@Sender Player player) {
        new KitAdminGUI(player).open();
    }

    @Subcommand(name = "create", description = "Create a kit from your inventory")
    @Permission("hiru.admin")
    public void create(@Sender Player player, @Named("name") String kitName,
                        @Optional("&f") @Named("displayName") @Combined String displayName) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) {
            Messages.send(player, "&cKit system is not available.");
            return;
        }

        if (kitManager.getKit(kitName) != null) {
            Messages.send(player, "&#FF4444A kit with name &f" + kitName + " &#FF4444already exists!");
            return;
        }

        // Collect items from player inventory (non-null, non-air)
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                items.add(stack.clone());
            }
        }

        if (items.isEmpty()) {
            Messages.send(player, "&#FF4444Your inventory is empty! Put items in your inventory first.");
            return;
        }

        Kit kit = new Kit(
                kitName,
                displayName,
                0,
                false,
                "hiru.kit." + kitName,
                Material.CHEST,
                Arrays.asList("&7Custom kit", "", "&#55FF55&l➥ Click to claim"),
                items
        );

        kitManager.registerKit(kit);
        Messages.send(player, "&#55FF55Kit &f" + kitName + " &#55FF55created with &f" + items.size() + " &#55FF55items!");
    }

    @Subcommand(name = "delete", description = "Delete a kit")
    @Permission("hiru.admin")
    public void delete(@Sender Player player, @Named("name") String kitName) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) {
            Messages.send(player, "&cKit system is not available.");
            return;
        }

        Kit kit = kitManager.getKit(kitName);
        if (kit == null) {
            Messages.send(player, "&#FF4444Kit &f" + kitName + " &#FF4444not found!");
            return;
        }

        kitManager.deleteKit(kitName);
        Messages.send(player, "&#55FF55Kit &f" + kitName + " &#55FF55deleted!");
    }

    @Subcommand(name = "setcooldown", description = "Set kit cooldown in seconds")
    @Permission("hiru.admin")
    public void setCooldown(@Sender Player player, @Named("kit") String kitName,
                            @Range(min = 0) @Named("seconds") int seconds) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) return;

        Kit oldKit = kitManager.getKit(kitName);
        if (oldKit == null) {
            Messages.send(player, "&#FF4444Kit &f" + kitName + " &#FF4444not found!");
            return;
        }

        Kit updated = new Kit(oldKit.getId(), oldKit.getDisplayName(), seconds,
                oldKit.isOneTime(), oldKit.getPermission(), oldKit.getIcon(), oldKit.getLore(), oldKit.getItems());
        kitManager.registerKit(updated);
        Messages.send(player, "&#55FF55Cooldown for &f" + kitName + " &#55FF55set to &f" + seconds + "s&#55FF55.");
    }

    @Subcommand(name = "seticon", description = "Set kit icon material")
    @Permission("hiru.admin")
    public void setIcon(@Sender Player player, @Named("kit") String kitName,
                        @Named("material") String materialName) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) return;

        Kit oldKit = kitManager.getKit(kitName);
        if (oldKit == null) {
            Messages.send(player, "&#FF4444Kit &f" + kitName + " &#FF4444not found!");
            return;
        }

        Material mat = Material.matchMaterial(materialName);
        if (mat == null) {
            Messages.send(player, "&#FF4444Invalid material: &f" + materialName);
            return;
        }

        Kit updated = new Kit(oldKit.getId(), oldKit.getDisplayName(), oldKit.getCooldown(),
                oldKit.isOneTime(), oldKit.getPermission(), mat, oldKit.getLore(), oldKit.getItems());
        kitManager.registerKit(updated);
        Messages.send(player, "&#55FF55Icon for &f" + kitName + " &#55FF55set to &f" + mat.name() + "&#55FF55.");
    }

    @Subcommand(name = "list", description = "List all kits")
    @Permission("hiru.admin")
    public void list(@Sender Player player) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null || kitManager.getKits().isEmpty()) {
            Messages.send(player, "&#8C8C8CNo kits defined.");
            return;
        }

        Messages.sendRaw(player, "&#FFD700&l⚡ Kit List:");
        for (Kit kit : kitManager.getKits()) {
            String status = kit.isOneTime() ? "&#FF8B76[one-time]" : "&#72FF97[" + kit.getCooldown() + "s]";
            Messages.sendRaw(player, " &8▸ " + Messages.color(kit.getDisplayName()) +
                    " &7(" + kit.getId() + ") " + Messages.color(status) +
                    " &7- " + kit.getItems().size() + " items");
        }
    }

    @Subcommand(name = "reload", description = "Reload kits from file")
    @Permission("hiru.admin")
    public void reload(@Sender Player player) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager != null) {
            kitManager.reload();
            Messages.send(player, "&#55FF55Kits reloaded! &f" + kitManager.getKits().size() + " &#55FF55kit(s) loaded.");
        }
    }
}
