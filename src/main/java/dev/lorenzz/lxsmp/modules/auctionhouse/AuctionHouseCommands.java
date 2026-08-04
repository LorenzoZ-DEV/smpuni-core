package dev.lorenzz.lxsmp.modules.auctionhouse;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("ah|auctionhouse|auction")
@Description("Open the Auction House")
public class AuctionHouseCommands extends BaseCommand {

    private final LXSMP plugin;
    private final AuctionHouseManager manager;
    private final AuctionHouseGUI gui;

    public AuctionHouseCommands(LXSMP plugin, AuctionHouseManager manager, AuctionHouseGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Default
    @CommandPermission("solum.auctionhouse.use")
    public void open(Player player) {
        gui.openMain(player, 0);
    }

    @Subcommand("sell")
    @CommandPermission("solum.auctionhouse.sell")
    @Description("List the item in your hand for sale")
    public void sell(Player player, double price) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            Messages.send(player, plugin.getConfigFile().getString("auctionhouse.messages.no-item",
                    "&cYou must hold an item to sell."));
            return;
        }
        AuctionHouseManager.ListResult r = manager.listItem(player, hand, price);
        switch (r) {
            case OK -> Messages.send(player, plugin.getConfigFile().getString("auctionhouse.messages.listed",
                    "&aListed for &e%price%&a!").replace("%price%", Messages.formatNumber(price)));
            case NO_ITEM -> Messages.send(player, plugin.getConfigFile().getString("auctionhouse.messages.no-item",
                    "&cYou must hold an item to sell."));
            case INVALID_PRICE -> Messages.send(player, plugin.getConfigFile().getString("auctionhouse.messages.invalid-price",
                            "&cPrice must be between &e%min% &cand &e%max%&c.")
                    .replace("%min%", Messages.formatNumber(manager.getMinPrice()))
                    .replace("%max%", Messages.formatNumber(manager.getMaxPrice())));
            case TOO_MANY_LISTINGS -> Messages.send(player, plugin.getConfigFile().getString("auctionhouse.messages.too-many-listings",
                    "&cYou have reached the maximum number of listings."));
            case NO_ECONOMY -> Messages.send(player, "&cEconomy not available.");
        }
    }

    @Subcommand("claim")
    @CommandPermission("solum.auctionhouse.use")
    @Description("Open your claim queue")
    public void claim(Player player) {
        gui.openClaim(player, 0);
    }

    @Subcommand("listings|mine")
    @CommandPermission("solum.auctionhouse.use")
    @Description("Show your active listings count")
    public void mine(Player player) {
        int count = manager.getListingsBySeller(player.getUniqueId()).size();
        Messages.send(player, "&7You have &b" + count + " &7active listing(s).");
    }
}

