package dev.lorenzz.lxsmp.modules.auctionhouse;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionHouseManager {

    private final LXSMP plugin;
    private final AuctionStorage storage;

    private final Map<UUID, AuctionListing> listings = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> claimQueue = new ConcurrentHashMap<>();

    private long defaultDurationMs;
    private int maxListingsPerPlayer;
    private double minPrice;
    private double maxPrice;
    private double taxPercent;

    private BukkitTask sweepTask;
    private Economy economy;

    public AuctionHouseManager(LXSMP plugin) {
        this.plugin = plugin;
        this.storage = new AuctionStorage(plugin);
    }

    public void init() {
        defaultDurationMs = Math.max(60_000L, plugin.getConfigFile().getLong("auctionhouse.duration-seconds", 86400L) * 1000L);
        maxListingsPerPlayer = plugin.getConfigFile().getInt("auctionhouse.max-listings-per-player", 5);
        minPrice = plugin.getConfigFile().getDouble("auctionhouse.min-price", 1.0);
        maxPrice = plugin.getConfigFile().getDouble("auctionhouse.max-price", 1_000_000_000.0);
        taxPercent = plugin.getConfigFile().getDouble("auctionhouse.tax-percent", 0.0);

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("[Solum-AH] No Vault economy found; AuctionHouse will not function.");
        } else {
            economy = rsp.getProvider();
        }

        storage.load();
        for (AuctionListing l : storage.loadAllListings()) {
            listings.put(l.id(), l);
        }
        claimQueue.putAll(storage.loadAllExpired());

        sweepTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sweepExpired, 600L, 600L);

        plugin.getLogger().info("[Solum] AuctionHouse loaded with " + listings.size() + " active listing(s).");
    }

    public void shutdown() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        for (AuctionListing l : listings.values()) storage.putListing(l);
        for (Map.Entry<UUID, List<ItemStack>> e : claimQueue.entrySet()) storage.putExpired(e.getKey(), e.getValue());
        storage.saveSync();
    }

    public Economy getEconomy() {
        return economy;
    }

    public Collection<AuctionListing> getListings() {
        return Collections.unmodifiableCollection(listings.values());
    }

    public List<AuctionListing> getActiveListingsSorted() {
        List<AuctionListing> list = new ArrayList<>(listings.values());
        list.sort(Comparator.comparingLong(AuctionListing::createdAt).reversed());
        return list;
    }

    public List<AuctionListing> getListingsBySeller(UUID seller) {
        List<AuctionListing> out = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.seller().equals(seller)) out.add(l);
        }
        return out;
    }

    public List<ItemStack> getClaimQueue(UUID owner) {
        return claimQueue.getOrDefault(owner, Collections.emptyList());
    }

    public enum ListResult { OK, NO_ITEM, INVALID_PRICE, TOO_MANY_LISTINGS, NO_ECONOMY }

    public ListResult listItem(Player seller, ItemStack item, double price) {
        if (economy == null) return ListResult.NO_ECONOMY;
        if (item == null || item.getType().isAir()) return ListResult.NO_ITEM;
        if (price < minPrice || price > maxPrice || Double.isNaN(price) || Double.isInfinite(price)) return ListResult.INVALID_PRICE;
        if (getListingsBySeller(seller.getUniqueId()).size() >= maxListingsPerPlayer
                && !seller.hasPermission("solum.auctionhouse.unlimited")) {
            return ListResult.TOO_MANY_LISTINGS;
        }

        ItemStack copy = item.clone();
        item.setAmount(0);

        long now = System.currentTimeMillis();
        AuctionListing listing = new AuctionListing(
                UUID.randomUUID(), seller.getUniqueId(), seller.getName(),
                copy, price, now, now + defaultDurationMs);
        listings.put(listing.id(), listing);
        storage.putListing(listing);
        storage.saveAsync();
        return ListResult.OK;
    }

    public enum BuyResult { OK, NOT_FOUND, OWN_LISTING, NO_FUNDS, INVENTORY_FULL, NO_ECONOMY }

    public BuyResult buy(Player buyer, UUID listingId) {
        if (economy == null) return BuyResult.NO_ECONOMY;
        AuctionListing listing = listings.get(listingId);
        if (listing == null || listing.isExpired()) return BuyResult.NOT_FOUND;
        if (listing.seller().equals(buyer.getUniqueId())) return BuyResult.OWN_LISTING;

        if (economy.getBalance(buyer) < listing.price()) return BuyResult.NO_FUNDS;
        if (buyer.getInventory().firstEmpty() == -1) return BuyResult.INVENTORY_FULL;

        EconomyResponse w = economy.withdrawPlayer(buyer, listing.price());
        if (!w.transactionSuccess()) return BuyResult.NO_FUNDS;

        double net = listing.price() * (1.0 - Math.max(0.0, Math.min(1.0, taxPercent / 100.0)));
        OfflinePlayer sellerOff = Bukkit.getOfflinePlayer(listing.seller());
        economy.depositPlayer(sellerOff, net);

        listings.remove(listingId);
        storage.removeListing(listingId);
        buyer.getInventory().addItem(listing.item().clone());

        Player sellerOnline = Bukkit.getPlayer(listing.seller());
        if (sellerOnline != null) {
            Messages.send(sellerOnline,
                    plugin.getConfigFile().getString("auctionhouse.messages.sold",
                            "&aYour listing was bought by &b%buyer% &afor &e%price%&a!")
                            .replace("%buyer%", buyer.getName())
                            .replace("%price%", Messages.formatNumber(net)));
        }

        storage.saveAsync();
        return BuyResult.OK;
    }

    public boolean cancel(Player seller, UUID listingId) {
        AuctionListing listing = listings.get(listingId);
        if (listing == null) return false;
        if (!listing.seller().equals(seller.getUniqueId()) && !seller.hasPermission("solum.auctionhouse.admin")) return false;
        listings.remove(listingId);
        storage.removeListing(listingId);
        addToClaimQueue(listing.seller(), listing.item());
        storage.saveAsync();
        return true;
    }

    private void sweepExpired() {
        long now = System.currentTimeMillis();
        List<AuctionListing> dead = new ArrayList<>();
        for (AuctionListing l : listings.values()) {
            if (l.expiresAt() <= now) dead.add(l);
        }
        if (dead.isEmpty()) return;
        for (AuctionListing l : dead) {
            listings.remove(l.id());
            storage.removeListing(l.id());
            addToClaimQueue(l.seller(), l.item());
        }
        storage.saveAsync();
    }

    private void addToClaimQueue(UUID owner, ItemStack item) {
        List<ItemStack> list = claimQueue.computeIfAbsent(owner, k -> Collections.synchronizedList(new ArrayList<>()));
        list.add(item.clone());
        storage.putExpired(owner, list);
    }

    public boolean claimOne(Player player, int index) {
        List<ItemStack> list = claimQueue.get(player.getUniqueId());
        if (list == null || list.isEmpty()) return false;
        if (index < 0 || index >= list.size()) return false;
        if (player.getInventory().firstEmpty() == -1) {
            Messages.send(player, plugin.getConfigFile().getString("auctionhouse.messages.inventory-full",
                    "&cYour inventory is full!"));
            return false;
        }
        ItemStack item = list.remove(index);
        player.getInventory().addItem(item);
        storage.putExpired(player.getUniqueId(), list);
        storage.saveAsync();
        return true;
    }

    public boolean claimAll(Player player) {
        List<ItemStack> list = claimQueue.get(player.getUniqueId());
        if (list == null || list.isEmpty()) return false;
        Iterator<ItemStack> it = list.iterator();
        boolean any = false;
        while (it.hasNext()) {
            if (player.getInventory().firstEmpty() == -1) break;
            player.getInventory().addItem(it.next());
            it.remove();
            any = true;
        }
        storage.putExpired(player.getUniqueId(), list);
        storage.saveAsync();
        return any;
    }

    public double getMinPrice() { return minPrice; }
    public double getMaxPrice() { return maxPrice; }
    public double getTaxPercent() { return taxPercent; }
    public long getDefaultDurationMs() { return defaultDurationMs; }
}

