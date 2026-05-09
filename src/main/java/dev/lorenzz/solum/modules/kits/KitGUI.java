package dev.lorenzz.solum.modules.kits;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitGUI implements InventoryHolder, Listener {

    public static final String COOLDOWN_BYPASS_PERMISSION = "solum.kits.cooldown.bypass";

    private static final int INNER_COLS = 7;
    private static final int MIN_INNER_ROWS = 3;
    private static final int MAX_INNER_ROWS = 4;

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, Kit> slotToKit = new HashMap<>();

    public KitGUI(Player player) {
        this.player = player;

        Collection<Kit> kits = KitManager.getInstance().getKits();
        int rowsNeeded = Math.max(1, (int) Math.ceil(kits.size() / (double) INNER_COLS));
        int innerRows = Math.max(MIN_INNER_ROWS, Math.min(MAX_INNER_ROWS, rowsNeeded));
        int size = (innerRows + 2) * 9;

        String title = Messages.color(Solum.getInstance().getConfigFile()
                .getString("kits.gui.title"));
        this.inventory = Bukkit.createInventory(this, size, title);

        rebuild();
        Bukkit.getPluginManager().registerEvents(this, Solum.getInstance());
    }

    private void rebuild() {
        slotToKit.clear();
        inventory.clear();

        int size = inventory.getSize();
        int rows = size / 9;

        ItemStack filler = createFiller();
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inventory.setItem(i, filler);
            }
        }

        KitPlayerDataManager data = KitPlayerDataManager.getInstance();
        List<Kit> kits = new ArrayList<>(KitManager.getInstance().getKits());

        int innerRows = rows - 2;
        int maxKits = innerRows * INNER_COLS;
        int count = Math.min(kits.size(), maxKits);
        if (count == 0) return;

        int rowsUsed = Math.max(1, (int) Math.ceil(count / (double) INNER_COLS));
        int startRow = 1 + (innerRows - rowsUsed) / 2;

        int base = count / rowsUsed;
        int extra = count % rowsUsed;

        int placed = 0;
        for (int r = 0; r < rowsUsed; r++) {
            int inThisRow = base + (r < extra ? 1 : 0);
            int leftPad = (INNER_COLS - inThisRow) / 2;
            int actualRow = startRow + r;
            for (int c = 0; c < inThisRow; c++) {
                int slot = actualRow * 9 + 1 + leftPad + c;
                Kit kit = kits.get(placed++);
                slotToKit.put(slot, kit);
                inventory.setItem(slot, buildIcon(kit, data));
            }
        }
    }

    private ItemStack createFiller() {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.setLore(Collections.emptyList());
            glass.setItemMeta(meta);
        }
        return glass;
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
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
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
        Kit kit = slotToKit.get(slot);
        if (kit == null) return;

        if (event.isRightClick()) {
            clicker.playSound(clicker.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.0f);
                    new KitPreviewGUI(clicker, kit).open();
            return;
        }

        if (event.isLeftClick()) {
            clicker.playSound(clicker.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
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
