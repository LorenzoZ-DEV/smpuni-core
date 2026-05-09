package dev.lorenzz.solum.modules.kits;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KitGUI implements InventoryHolder, Listener {

    public static final String COOLDOWN_BYPASS_PERMISSION = "solum.kits.cooldown.bypass";

    private final Player player;
    private final Inventory inventory;
    private final List<Kit> indexed = new ArrayList<>();

    public KitGUI(Player player) {
        this.player = player;

        Collection<Kit> kits = KitManager.getInstance().getKits();
        int rows = Math.max(1, Math.min(6, (int) Math.ceil(kits.size() / 9.0)));
        int size = rows * 9;

        String title = Messages.color(Solum.getInstance().getConfigFile()
                .getString("kits.gui.title"));
        this.inventory = Bukkit.createInventory(this, size, title);

        rebuild();
        Bukkit.getPluginManager().registerEvents(this, Solum.getInstance());
    }

    private void rebuild() {
        indexed.clear();
        inventory.clear();

        KitPlayerDataManager data = KitPlayerDataManager.getInstance();
        int slot = 0;
        for (Kit kit : KitManager.getInstance().getKits()) {
            if (slot >= inventory.getSize()) break;
            indexed.add(kit);
            inventory.setItem(slot, buildIcon(kit, data));
            slot++;
        }
    }

    private ItemStack buildIcon(Kit kit, KitPlayerDataManager data) {
        boolean hasPerm = kit.permission().isEmpty() || player.hasPermission(kit.permission());
        boolean oneTimeUsed = kit.oneTime() && data.hasClaimedOneTime(player.getUniqueId(), kit.id());
        boolean bypass = player.hasPermission(COOLDOWN_BYPASS_PERMISSION);
        long remaining = bypass ? 0 : data.getRemainingCooldown(player.getUniqueId(), kit);

        boolean claimable = hasPerm && !oneTimeUsed && remaining <= 0;

        ItemStack icon = new ItemStack(claimable ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = icon.getItemMeta();
        if (meta == null) return icon;

        meta.setDisplayName(Messages.color(kit.displayName()));

        List<String> lore = new ArrayList<>();
        for (String line : kit.lore()) lore.add(Messages.color(line));
        lore.add("");

        if (!hasPerm) {
            lore.add(Messages.color("&#FF4444✖ No permission"));
        } else if (oneTimeUsed) {
            lore.add(Messages.color("&#FF8B76✔ Already claimed"));
        } else if (remaining > 0) {
            lore.add(Messages.color("&#FFD700⏱ Cooldown: &f"
                    + KitPlayerDataManager.formatCooldown(remaining)));
        } else {
            lore.add(Messages.color("&#55FF55✔ Available"));
        }

        lore.add("");
        lore.add(Messages.color("&7Items: &f" + kit.items().size()));
        if (bypass && kit.cooldown() > 0) {
        }
        lore.add("");
        lore.add(Messages.color("&aLeft-click &7to claim"));
        lore.add(Messages.color("&bRight-click &7to preview"));
        meta.setLore(lore);

        icon.setItemMeta(meta);
        return icon;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!this.equals(event.getInventory().getHolder())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= indexed.size()) return;

        Kit kit = indexed.get(slot);
        if (kit == null) return;

        if (event.isRightClick()) {
            Bukkit.getScheduler().runTask(Solum.getInstance(), () ->
                    new KitPreviewGUI(clicker, kit).open());
            return;
        }

        if (event.isLeftClick()) {
            KitCommand.claimKit(clicker, kit, KitPlayerDataManager.getInstance());
            rebuild();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (this.equals(event.getInventory().getHolder())) {
            HandlerList.unregisterAll(this);
        }
    }
}
