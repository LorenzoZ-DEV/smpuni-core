package dev.lorenzz.solum.modules.auctionhouse;

import dev.lorenzz.solum.Solum;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Persistent YAML storage for AuctionHouse listings and per-seller expired/unclaimed items.
 *
 * File layout (data/auctions.yml):
 * listings:
 *   <id>:
 *     seller: <uuid>
 *     seller-name: <name>
 *     item: <base64 byte[]>
 *     price: <double>
 *     created: <epoch ms>
 *     expires: <epoch ms>
 * expired:
 *   <seller-uuid>:
 *     - <base64 byte[]>
 */
public class AuctionStorage {

    private final Solum plugin;
    private final File file;
    private YamlConfiguration yaml;

    public AuctionStorage(Solum plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists()) //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        this.file = new File(dir, "auctions.yml");
    }

    public void load() {
        try {
            if (!file.exists()) //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            yaml = YamlConfiguration.loadConfiguration(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Solum-AH] Failed to load auctions.yml: " + e.getMessage());
            yaml = new YamlConfiguration();
        }
    }

    public synchronized void saveAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    public synchronized void saveSync() {
        if (yaml == null) return;
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[Solum-AH] Failed to save auctions.yml: " + e.getMessage());
        }
    }

    /* -------- Listings -------- */

    public List<AuctionListing> loadAllListings() {
        List<AuctionListing> out = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection("listings");
        if (root == null) return out;
        for (String key : root.getKeys(false)) {
            try {
                ConfigurationSection s = root.getConfigurationSection(key);
                if (s == null) continue;
                UUID id = UUID.fromString(key);
                UUID seller = UUID.fromString(s.getString("seller"));
                String sellerName = s.getString("seller-name", "Unknown");
                ItemStack item = ItemSerialization.deserialize(s.getString("item"));
                if (item == null) continue;
                double price = s.getDouble("price", 0.0);
                long created = s.getLong("created", System.currentTimeMillis());
                long expires = s.getLong("expires", System.currentTimeMillis());
                out.add(new AuctionListing(id, seller, sellerName, item, price, created, expires));
            } catch (Exception ex) {
                plugin.getLogger().warning("[Solum-AH] Skipped corrupt listing '" + key + "': " + ex.getMessage());
            }
        }
        return out;
    }

    public synchronized void putListing(AuctionListing listing) {
        String base = "listings." + listing.id();
        yaml.set(base + ".seller", listing.seller().toString());
        yaml.set(base + ".seller-name", listing.sellerName());
        yaml.set(base + ".item", ItemSerialization.serialize(listing.item()));
        yaml.set(base + ".price", listing.price());
        yaml.set(base + ".created", listing.createdAt());
        yaml.set(base + ".expires", listing.expiresAt());
    }

    public synchronized void removeListing(UUID id) {
        yaml.set("listings." + id, null);
    }

    /* -------- Expired (per-seller claim queue) -------- */

    public Map<UUID, List<ItemStack>> loadAllExpired() {
        Map<UUID, List<ItemStack>> out = new HashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("expired");
        if (root == null) return out;
        for (String key : root.getKeys(false)) {
            try {
                UUID owner = UUID.fromString(key);
                List<String> raw = root.getStringList(key);
                List<ItemStack> items = new ArrayList<>();
                for (String s : raw) {
                    ItemStack i = ItemSerialization.deserialize(s);
                    if (i != null) items.add(i);
                }
                out.put(owner, items);
            } catch (Exception ignored) {}
        }
        return out;
    }

    public synchronized void putExpired(UUID owner, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            yaml.set("expired." + owner, null);
            return;
        }
        List<String> raw = new ArrayList<>(items.size());
        for (ItemStack i : items) {
            if (i != null) raw.add(ItemSerialization.serialize(i));
        }
        yaml.set("expired." + owner, raw);
    }
}

