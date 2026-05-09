package dev.lorenzz.solum.modules.auctionhouse;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a single listing on the Auction House.
 */
public class AuctionListing {

    private final UUID id;
    private final UUID seller;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long createdAt;
    private final long expiresAt;

    public AuctionListing(UUID id, UUID seller, String sellerName, ItemStack item,
                          double price, long createdAt, long expiresAt) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getSeller() { return seller; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item; }
    public double getPrice() { return price; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public long getMillisRemaining() {
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }
}

