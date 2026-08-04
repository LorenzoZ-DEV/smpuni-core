package dev.lorenzz.lxsmp.modules.kits;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandAlias("kitadmin")
@CommandPermission("solum.admin")
@Description("Kit administration panel")
public class KitAdminCommand extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void kitadmin(Player player) {
        new KitAdminGUI(player).open();
    }

    @Subcommand("create")
    @Description("Create a kit from your inventory")
    public void create(Player player, String kitName, @Optional String[] displayNameArgs) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) {
            Messages.send(player, "&cKit system is not available.");
            return;
        }

        if (kitManager.getKit(kitName) != null) {
            Messages.send(player, "&#FF4444A kit with name &f" + kitName + " &#FF4444already exists!");
            return;
        }

        String displayName = (displayNameArgs == null || displayNameArgs.length == 0)
                ? "&f" : String.join(" ", displayNameArgs);

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
                "solum.kit." + kitName,
                Material.CHEST,
                Arrays.asList("&7Custom kit", "", "&#55FF55&l➥ Click to claim"),
                items
        );

        kitManager.registerKit(kit);
        Messages.send(player, "&#55FF55Kit &f" + kitName + " &#55FF55created with &f" + items.size() + " &#55FF55items!");
    }

    @Subcommand("delete")
    @Description("Delete a kit")
    public void delete(Player player, String kitName) {
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

    @Subcommand("setcooldown")
    @Description("Set kit cooldown in seconds")
    public void setCooldown(Player player, String kitName, int seconds) {
        if (seconds < 0) { Messages.send(player, "&cSeconds cannot be negative!"); return; }
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null) return;

        Kit oldKit = kitManager.getKit(kitName);
        if (oldKit == null) {
            Messages.send(player, "&#FF4444Kit &f" + kitName + " &#FF4444not found!");
            return;
        }

        Kit updated = new Kit(oldKit.id(), oldKit.displayName(), seconds,
                oldKit.oneTime(), oldKit.permission(), oldKit.icon(), oldKit.lore(), oldKit.items());
        kitManager.registerKit(updated);
        Messages.send(player, "&#55FF55Cooldown for &f" + kitName + " &#55FF55set to &f" + seconds + "s&#55FF55.");
    }

    @Subcommand("seticon")
    @Description("Set kit icon material")
    public void setIcon(Player player, String kitName, String materialName) {
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

        Kit updated = new Kit(oldKit.id(), oldKit.displayName(), oldKit.cooldown(),
                oldKit.oneTime(), oldKit.permission(), mat, oldKit.lore(), oldKit.items());
        kitManager.registerKit(updated);
        Messages.send(player, "&#55FF55Icon for &f" + kitName + " &#55FF55set to &f" + mat.name() + "&#55FF55.");
    }

    @Subcommand("list")
    @Description("List all kits")
    public void list(Player player) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager == null || kitManager.getKits().isEmpty()) {
            Messages.send(player, "&#8C8C8CNo kits defined.");
            return;
        }

        Messages.sendRaw(player, "&#FFD700&l⚡ Kit List:");
        for (Kit kit : kitManager.getKits()) {
            String status = kit.oneTime() ? "&#FF8B76[one-time]" : "&#72FF97[" + kit.cooldown() + "s]";
            Messages.sendRaw(player, " &8▸ " + Messages.color(kit.displayName()) +
                    " &7(" + kit.id() + ") " + Messages.color(status) +
                    " &7- " + kit.items().size() + " items");
        }
    }

    @Subcommand("reload")
    @Description("Reload kits from file")
    public void reload(Player player) {
        KitManager kitManager = KitManager.getInstance();
        if (kitManager != null) {
            kitManager.reload();
            Messages.send(player, "&#55FF55Kits reloaded! &f" + kitManager.getKits().size() + " &#55FF55kit(s) loaded.");
        }
    }
}

