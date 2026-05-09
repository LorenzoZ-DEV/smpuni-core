package dev.lorenzz.solum.modules.auctionhouse;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.GUI;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AuctionHouseGUI {

    private static final int PAGE_SIZE = 28;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final Solum plugin;
    private final AuctionHouseManager manager;

    public AuctionHouseGUI(Solum plugin, AuctionHouseManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void openMain(Player viewer, int page) {
        List<AuctionListing> all = manager.getActiveListingsSorted();
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, all.size());
        List<AuctionListing> visible = new ArrayList<>(all.subList(from, to));

        String title = plugin.getConfigFile().getString("auctionhouse.gui.title", "&8Auction House &7(%page%/%pages%)")
                .replace("%page%", String.valueOf(safePage + 1))
                .replace("%pages%", String.valueOf(totalPages));

        GUI.Builder b = GUI.builder(title, 6).border(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < visible.size(); i++) {
            AuctionListing l = visible.get(i);
            int slot = CONTENT_SLOTS[i];
            ItemStack display = buildListingDisplay(l, viewer);
            final int currentPage = safePage;
            b.item(slot, display, p -> {
                boolean ownsIt = l.seller().equals(p.getUniqueId());
                if (ownsIt) {
                    if (manager.cancel(p, l.id())) {
                        Messages.send(p, plugin.getConfigFile().getString("auctionhouse.messages.cancelled",
                                "&aListing cancelled. Item moved to your claim queue."));
                    } else {
                        Messages.send(p, plugin.getConfigFile().getString("auctionhouse.messages.cancel-failed",
                                "&cCould not cancel that listing."));
                    }
                } else {
                    handleBuy(p, l);
                }
                openMain(p, currentPage);
            });
        }

        if (visible.isEmpty()) {
            b.item(22, Material.BARRIER, "&cNo active listings",
                    List.of("&7Be the first! Use &b/ah sell <price>",
                            "&7holding the item you want to list."));
        }

        if (safePage > 0) {
            b.item(45, Material.ARROW, "&aPrevious page", List.of("&7Click to go back"),
                    p -> openMain(p, safePage - 1));
        }
        b.item(48, Material.CHEST, "&eYour claim queue",
                List.of("&7Items waiting to be reclaimed: &b" + manager.getClaimQueue(viewer.getUniqueId()).size(),
                        "",
                        "&aClick to open"),
                p -> openClaim(p, 0));
        b.item(49, buildInfoItem(viewer));
        b.item(50, Material.BARRIER, "&cClose", List.of(), Player::closeInventory);
        if (safePage < totalPages - 1) {
            b.item(53, Material.ARROW, "&aNext page", List.of("&7Click to advance"),
                    p -> openMain(p, safePage + 1));
        }

        b.open(viewer);
    }

    private void handleBuy(Player buyer, AuctionListing listing) {
        AuctionHouseManager.BuyResult r = manager.buy(buyer, listing.id());
        switch (r) {
            case OK -> Messages.send(buyer, plugin.getConfigFile().getString("auctionhouse.messages.bought",
                            "&aPurchased &b%item% &afor &e%price%&a!")
                    .replace("%item%", itemName(listing.item()))
                    .replace("%price%", Messages.formatNumber(listing.price())));
            case NOT_FOUND -> Messages.send(buyer, plugin.getConfigFile().getString("auctionhouse.messages.not-found",
                    "&cThat listing is no longer available."));
            case OWN_LISTING -> Messages.send(buyer, plugin.getConfigFile().getString("auctionhouse.messages.own-listing",
                    "&cYou cannot buy your own listing."));
            case NO_FUNDS -> Messages.send(buyer, plugin.getConfigFile().getString("auctionhouse.messages.no-funds",
                    "&cYou don't have enough money."));
            case INVENTORY_FULL -> Messages.send(buyer, plugin.getConfigFile().getString("auctionhouse.messages.inventory-full",
                    "&cYour inventory is full!"));
            case NO_ECONOMY -> Messages.send(buyer, "&cEconomy not available.");
        }
    }

    public void openClaim(Player viewer, int page) {
        List<ItemStack> items = new ArrayList<>(manager.getClaimQueue(viewer.getUniqueId()));
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIdx = safePage * PAGE_SIZE;
        int toIdx = Math.min(fromIdx + PAGE_SIZE, items.size());
        List<ItemStack> visible = new ArrayList<>(items.subList(fromIdx, toIdx));

        String title = plugin.getConfigFile().getString("auctionhouse.gui.claim-title",
                        "&8Claim Queue &7(%page%/%pages%)")
                .replace("%page%", String.valueOf(safePage + 1))
                .replace("%pages%", String.valueOf(totalPages));

        GUI.Builder b = GUI.builder(title, 6).border(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < visible.size(); i++) {
            final int globalIndex = fromIdx + i;
            int slot = CONTENT_SLOTS[i];
            ItemStack display = withAppendedLore(visible.get(i).clone(), List.of("", Messages.color("&aClick to claim")));
            b.item(slot, display, p -> {
                manager.claimOne(p, globalIndex);
                openClaim(p, safePage);
            });
        }

        if (visible.isEmpty()) {
            b.item(22, Material.BARRIER, "&7Nothing to claim", List.of("&8Your queue is empty."));
        }

        if (safePage > 0) {
            b.item(45, Material.ARROW, "&aPrevious page", List.of(),
                    p -> openClaim(p, safePage - 1));
        }
        b.item(48, Material.HOPPER, "&aClaim All",
                List.of("&7Click to claim everything you can"),
                p -> {
                    manager.claimAll(p);
                    openClaim(p, 0);
                });
        b.item(49, Material.BOOK, "&eAuction House",
                List.of("&7Click to go back"),
                p -> openMain(p, 0));
        b.item(50, Material.BARRIER, "&cClose", List.of(), Player::closeInventory);
        if (safePage < totalPages - 1) {
            b.item(53, Material.ARROW, "&aNext page", List.of(),
                    p -> openClaim(p, safePage + 1));
        }

        b.open(viewer);
    }

    private ItemStack buildListingDisplay(AuctionListing listing, Player viewer) {
        ItemStack display = listing.item().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;
        List<String> lore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) lore.addAll(meta.getLore());
        if (!lore.isEmpty()) lore.add(Messages.color("&8&m                              "));
        lore.add(Messages.color("&7Seller: &f" + listing.sellerName()));
        lore.add(Messages.color("&7Price: &e" + Messages.formatNumber(listing.price())));
        lore.add(Messages.color("&7Expires in: &b" + formatRemaining(listing.getMillisRemaining())));
        lore.add("");
        if (listing.seller().equals(viewer.getUniqueId())) {
            lore.add(Messages.color("&cClick to cancel"));
        } else {
            lore.add(Messages.color("&aClick to buy"));
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private ItemStack buildInfoItem(Player viewer) {
        int yourListings = manager.getListingsBySeller(viewer.getUniqueId()).size();
        int total = manager.getListings().size();
        int claim = manager.getClaimQueue(viewer.getUniqueId()).size();
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&eAuction House"));
            meta.setLore(List.of(
                    Messages.color("&7Active listings: &b" + total),
                    Messages.color("&7Your listings: &b" + yourListings),
                    Messages.color("&7Pending claims: &b" + claim),
                    "",
                    Messages.color("&7Use &b/ah sell <price> &7to list"),
                    Messages.color("&7the item in your hand.")
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack withAppendedLore(ItemStack base, List<String> extra) {
        ItemMeta meta = base.getItemMeta();
        if (meta == null) return base;
        List<String> lore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) lore.addAll(meta.getLore());
        lore.addAll(extra);
        meta.setLore(lore);
        base.setItemMeta(meta);
        return base;
    }

    private String itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.getDisplayName() != null && !meta.getDisplayName().isEmpty()) {
            return meta.getDisplayName();
        }
        String raw = item.getType().name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1) + " x" + item.getAmount();
    }

    private String formatRemaining(long ms) {
        long s = ms / 1000L;
        long h = s / 3600L; s %= 3600L;
        long m = s / 60L; s %= 60L;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}

