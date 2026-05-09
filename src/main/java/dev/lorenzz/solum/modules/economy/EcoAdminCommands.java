package dev.lorenzz.solum.modules.economy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.EconomyManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@CommandAlias("eco|economy")
@CommandPermission("solum.admin")
@Description("Economy admin management")
public class EcoAdminCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void ecoRoot(CommandSender sender) {
        Messages.send(sender, "&cUsage: /eco <give|take|set> <player> <amount>");
    }

    @Subcommand("give")
    @Description("Give money to a player")
    public void ecoGive(CommandSender sender, OfflinePlayer target, double amount) {
        if (amount < 1) { Messages.send(sender, "&cAmount must be at least 1!"); return; }
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.addBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-give", "&7Given %symbol%%amount% to %player%.")
                .replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(amount)).replace("%player%", name));
    }

    @Subcommand("take")
    @Description("Take money from a player")
    public void ecoTake(CommandSender sender, OfflinePlayer target, double amount) {
        if (amount < 1) { Messages.send(sender, "&cAmount must be at least 1!"); return; }
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.removeBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-take", "&7Removed %symbol%%amount% from %player%.")
                .replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(amount)).replace("%player%", name));
    }

    @Subcommand("set")
    @Description("Set a player's balance")
    public void ecoSet(CommandSender sender, OfflinePlayer target, double amount) {
        if (amount < 0) { Messages.send(sender, "&cAmount cannot be negative!"); return; }
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.setBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-set", "&7Balance of %player% set to %symbol%%amount%.")
                .replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(amount)).replace("%player%", name));
    }
}

