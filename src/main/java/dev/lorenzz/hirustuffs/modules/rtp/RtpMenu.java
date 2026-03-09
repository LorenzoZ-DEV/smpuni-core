package dev.lorenzz.hirustuffs.modules.rtp;

import com.cryptomorin.xseries.XMaterial;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.ModuleManager;
import dev.lorenzz.hirustuffs.utils.Config;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class RtpMenu implements InventoryHolder, Listener {

    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, String> slotActions = new HashMap<>();
    private final Hiru plugin;

    public RtpMenu(Player player) {
        this.player = player;
        this.plugin = Hiru.getInstance();

        Config config = plugin.getConfigFile();
        int rows = config.getInt("rtp.menu.rows", 3);
        String title = Messages.color(config.getString("rtp.menu.title", "&0Random Teleport"));

        this.inventory = Bukkit.createInventory(this, rows * 9, title);

        setupInventory();
        registerEvents();
    }

    private void setupInventory() {
        Config config = plugin.getConfigFile();

        // Fill with filler
        String fillerMaterialName = config.getString("rtp.menu.filler.material", "GRAY_STAINED_GLASS_PANE");
        String fillerName = config.getString("rtp.menu.filler.name", "&f");
        ItemStack filler = createFiller(fillerMaterialName, fillerName);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Load world buttons from config
        ConfigurationSection buttonsSection = config.getConfigurationSection("rtp.menu.buttons");
        if (buttonsSection == null) return;

        for (String key : buttonsSection.getKeys(false)) {
            ConfigurationSection buttonConfig = buttonsSection.getConfigurationSection(key);
            if (buttonConfig == null) continue;

            int slot = buttonConfig.getInt("slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) continue;

            String targetWorld = buttonConfig.getString("target-world", "world");
            boolean enabled = buttonConfig.getBoolean("enabled", true);
            if (!enabled) continue;

            String texture = buttonConfig.getString("texture", "");
            String name = buttonConfig.getString("name", "&7" + key);
            List<String> lore = buttonConfig.getStringList("lore");

            ItemStack button;
            if (!texture.isEmpty()) {
                button = createSkullButton(texture, name, lore);
            } else {
                String materialName = buttonConfig.getString("material", "PLAYER_HEAD");
                button = createMaterialButton(materialName, name, lore);
            }

            inventory.setItem(slot, button);
            slotActions.put(slot, targetWorld);
        }
    }

    private ItemStack createFiller(String materialName, String displayName) {
        ItemStack filler;
        XMaterial xMat = XMaterial.matchXMaterial(materialName).orElse(null);
        if (xMat != null) {
            filler = xMat.parseItem();
        } else {
            filler = null;
        }
        if (filler == null) {
            filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        }
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(displayName));
            filler.setItemMeta(meta);
        }
        return filler;
    }

    private ItemStack createSkullButton(String texture, String name, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", texture));
                meta.setPlayerProfile(profile);
            } catch (Exception e) {
                plugin.getLogger().warning("[Hiru] Failed to apply skull texture: " + e.getMessage());
            }
            meta.setDisplayName(Messages.color(name));
            meta.setLore(colorLore(lore));
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private ItemStack createMaterialButton(String materialName, String name, List<String> lore) {
        ItemStack item;
        XMaterial xMat = XMaterial.matchXMaterial(materialName).orElse(null);
        if (xMat != null) {
            item = xMat.parseItem();
        } else {
            item = null;
        }
        if (item == null) {
            item = new ItemStack(Material.PLAYER_HEAD);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            meta.setLore(colorLore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> colorLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) return Collections.emptyList();
        List<String> colored = new ArrayList<>(lore.size());
        for (String line : lore) {
            colored.add(Messages.color(line));
        }
        return colored;
    }

    private void registerEvents() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        if (!this.equals(event.getInventory().getHolder())) return;

        event.setCancelled(true);

        if (event.getClick() != ClickType.LEFT) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        String targetWorld = slotActions.get(slot);
        if (targetWorld == null) return;

        clicker.closeInventory();

        RTPModule rtpModule = ModuleManager.getInstance().getModule("rtp");
        if (rtpModule == null) return;

        RandomTeleportService rtpService = rtpModule.getRtpService();
        if (rtpService != null) {
            rtpService.handleCommand(clicker, targetWorld);
        } else {
            Messages.send(clicker, "&cRTP service is not available!");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (this.equals(event.getInventory().getHolder())) {
            HandlerList.unregisterAll(this);
        }
    }
}
