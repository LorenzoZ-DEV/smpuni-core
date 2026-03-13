package dev.lorenzz.hirustuffs.modules.economy;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class EcoAdminCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "eco", aliases = {"economy"}, description = "Economy admin management")
    @Permission("hiru.admin")
    public void ecoRoot(@Sender CommandSender sender) {
        Messages.send(sender, "&cUsage: /eco <give|take|set> <player> <amount>");
    }

    @Subcommand(name = "give", description = "Give money to a player")
    @Permission("hiru.admin")
    public void ecoGive(@Sender CommandSender sender, @Named("player") OfflinePlayer target, @Range(min = 1) @Named("amount") double amount) {
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.addBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-give", "&7Given %symbol%%amount% to %player%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", name));
    }

    @Subcommand(name = "take", description = "Take money from a player")
    @Permission("hiru.admin")
    public void ecoTake(@Sender CommandSender sender, @Named("player") OfflinePlayer target, @Range(min = 1) @Named("amount") double amount) {
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.removeBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-take", "&7Removed %symbol%%amount% from %player%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", name));
    }

    @Subcommand(name = "set", description = "Set a player's balance")
    @Permission("hiru.admin")
    public void ecoSet(@Sender CommandSender sender, @Named("player") OfflinePlayer target, @Range(min = 0) @Named("amount") double amount) {
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.setBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-set", "&7Balance of %player% set to %symbol%%amount%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", name));
    }
}
