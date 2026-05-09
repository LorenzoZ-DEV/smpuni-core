package dev.lorenzz.solum.modules.auctionhouse;

import com.cryptomorin.xseries.XMaterial;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import net.j4c0b3y.api.menu.Menu;
import net.j4c0b3y.api.menu.MenuSize;
import net.j4c0b3y.api.menu.button.impl.PlaceholderButton;
import net.j4c0b3y.api.menu.button.impl.SimpleButton;
import net.j4c0b3y.api.menu.layer.impl.BackgroundLayer;
import net.j4c0b3y.api.menu.layer.impl.ForegroundLayer;
import net.j4c0b3y.api.menu.utils.Item;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI rendering for /ah and /ah claim. Pagination is handled by reopening the
 * menu with an updated page index.
 */
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

    /* -------- Main /ah view -------- */

    public void openMain(Player viewer, int page) {
        List<AuctionListing> all = manager.getActiveListingsSorted();
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, all.size());
        List<AuctionListing> visible = all.subList(from, to);

        String title = plugin.getConfigFile().getString("auctionhouse.gui.title", "&8Auction House &7(%page%/%pages%)")
                .replace("%page%", String.valueOf(safePage + 1))
                .replace("%pages%", String.valueOf(totalPages));

        new Menu(Messages.color(title), MenuSize.of(6), viewer) {
            @Override
            public void setup(BackgroundLayer bg, ForegroundLayer fg) {
                bg.fill(new PlaceholderButton(XMaterial.BLACK_STAINED_GLASS_PANE));

                // Listings
                for (int i = 0; i < visible.size(); i++) {
                    AuctionListing l = visible.get(i);
                    int slot = CONTENT_SLOTS[i];
                    fg.set(slot, new SimpleButton(buildListingDisplay(l, viewer), click -> {
                        Player p = click.getMenu().getPlayer();
                        boolean ownsIt = l.getSeller().equals(p.getUniqueId());
                        if (ownsIt) {
                            // Cancel
                            if (manager.cancel(p, l.getId())) {
                                Messages.send(p, plugin.getConfigFile().getString("auctionhouse.messages.cancelled",
                                        "&aListing cancelled. Item moved to your claim queue."));
                            } else {
                                Messages.send(p, plugin.getConfigFile().getString("auctionhouse.messages.cancel-failed",
                                        "&cCould not cancel that listing."));
                            }
                            click.getMenu().close();
                            openMain(p, safePage);
                        } else {
                            // Buy
                            handleBuy(p, l);
                            click.getMenu().close();
                            openMain(p, safePage);
                        }
                    }));
                }

                // Navigation row (slots 45..53)
                if (safePage > 0) {
                    fg.set(45, new SimpleButton(simpleItem(Material.ARROW, "&aPrevious page",
                            List.of("&7Click to go back")),
                            click -> openMain(click.getMenu().getPlayer(), safePage - 1)));
                }
                fg.set(49, new SimpleButton(buildInfoItem(viewer)));
                fg.set(48, new SimpleButton(simpleItem(Material.CHEST, "&eYour claim queue",
                        List.of("&7Items waiting to be reclaimed: &b" + manager.getClaimQueue(viewer.getUniqueId()).size(),
                                "",
                                "&aClick to open")),
                        click -> openClaim(click.getMenu().getPlayer(), 0)));
                fg.set(50, new SimpleButton(simpleItem(Material.BARRIER, "&cClose", List.of()),
                        click -> click.getMenu().close()));
                if (safePage < totalPages - 1) {
                    fg.set(53, new SimpleButton(simpleItem(Material.ARROW, "&aNext page",
                            List.of("&7Click to advance")),
                            click -> openMain(click.getMenu().getPlayer(), safePage + 1)));
                }
            }
        }.open();
    }

    private void handleBuy(Player buyer, AuctionListing listing) {
        AuctionHouseManager.BuyResult r = manager.buy(buyer, listing.getId());
        switch (r) {
            case OK -> Messages.send(buyer, plugin.getConfigFile().getString("auctionhouse.messages.bought",
                    "&aPurchased &b%item% &afor &e%price%&a!")
                    .replace("%item%", itemName(listing.getItem()))
                    .replace("%price%", Messages.formatNumber(listing.getPrice())));
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

    /* -------- Claim queue view -------- */

    public void openClaim(Player viewer, int page) {
        List<ItemStack> items = new ArrayList<>(manager.getClaimQueue(viewer.getUniqueId()));
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int from = safePage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, items.size());
        List<ItemStack> visible = items.subList(from, to);

        String title = plugin.getConfigFile().getString("auctionhouse.gui.claim-title",
                "&8Claim Queue &7(%page%/%pages%)")
                .replace("%page%", String.valueOf(safePage + 1))
                .replace("%pages%", String.valueOf(totalPages));

        new Menu(Messages.color(title), MenuSize.of(6), viewer) {
            @Override
            public void setup(BackgroundLayer bg, ForegroundLayer fg) {
                bg.fill(new PlaceholderButton(XMaterial.BLACK_STAINED_GLASS_PANE));

                for (int i = 0; i < visible.size(); i++) {
                    final int globalIndex = from + i;
                    int slot = CONTENT_SLOTS[i];
                    ItemStack base = visible.get(i).clone();
                    ItemStack display = withAppendedLore(base, List.of("", Messages.color("&aClick to claim")));
                    fg.set(slot, new SimpleButton(display, click -> {
                        Player p = click.getMenu().getPlayer();
                        manager.claimOne(p, globalIndex);
                        click.getMenu().close();
                        openClaim(p, safePage);
                    }));
                }

                if (safePage > 0) {
                    fg.set(45, new SimpleButton(simpleItem(Material.ARROW, "&aPrevious page", List.of()),
                            click -> openClaim(click.getMenu().getPlayer(), safePage - 1)));
                }
                fg.set(48, new SimpleButton(simpleItem(Material.HOPPER, "&aClaim All",
                                List.of("&7Click to claim everything you can")),
                        click -> {
                            manager.claimAll(click.getMenu().getPlayer());
                            click.getMenu().close();
                            openClaim(click.getMenu().getPlayer(), 0);
                        }));
                fg.set(49, new SimpleButton(simpleItem(Material.BOOK, "&eAuction House",
                                List.of("&7Click to go back")),
                        click -> openMain(click.getMenu().getPlayer(), 0)));
                fg.set(50, new SimpleButton(simpleItem(Material.BARRIER, "&cClose", List.of()),
                        click -> click.getMenu().close()));
                if (safePage < totalPages - 1) {
                    fg.set(53, new SimpleButton(simpleItem(Material.ARROW, "&aNext page", List.of()),
                            click -> openClaim(click.getMenu().getPlayer(), safePage + 1)));
                }
            }
        }.open();
    }

    /* -------- Item builders -------- */

    private ItemStack buildListingDisplay(AuctionListing listing, Player viewer) {
        ItemStack display = listing.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;
        List<String> lore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) lore.addAll(meta.getLore());
        if (!lore.isEmpty()) lore.add(Messages.color("&8&m                              "));
        lore.add(Messages.color("&7Seller: &f" + listing.getSellerName()));
        lore.add(Messages.color("&7Price: &e" + Messages.formatNumber(listing.getPrice())));
        lore.add(Messages.color("&7Expires in: &b" + formatRemaining(listing.getMillisRemaining())));
        lore.add("");
        if (listing.getSeller().equals(viewer.getUniqueId())) {
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
        return simpleItem(Material.PAPER, "&eAuction House",
                List.of("&7Active listings: &b" + total,
                        "&7Your listings: &b" + yourListings,
                        "&7Pending claims: &b" + claim,
                        "",
                        "&7Use &b/ah sell <price> &7to list",
                        "&7the item in your hand."));
    }

    private ItemStack simpleItem(Material material, String name, List<String> lore) {
        Item item = new Item(XMaterial.matchXMaterial(material));
        item.setName(Messages.color(name));
        if (lore != null && !lore.isEmpty()) {
            item.setLore(lore.stream().map(Messages::color).toList());
        }
        return item.build();
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

