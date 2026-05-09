package dev.lorenzz.solum.modules.auctionhouse;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents a single listing on the Auction House.
 */
public record AuctionListing(UUID id, UUID seller, String sellerName, ItemStack item, double price, long createdAt,
                             long expiresAt) {

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }

    public long getMillisRemaining() {
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }
}

