package dev.lorenzz.hirustuffs.modules.economy;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyCommands {

    private final Hiru plugin = Hiru.getInstance();

    // /balance [player]
    @Command(name = "balance", aliases = {"bal", "money"}, description = "Controlla il saldo")
    public void balance(@Sender CommandSender sender, @Optional @Named("giocatore") Player target) {
        EconomyManager eco = EconomyManager.getInstance();
        if (eco == null) { Messages.send(sender, "&cEconomy non disponibile."); return; }

        String symbol = eco.getCurrencySymbol();
        if (target != null) {
            double bal = eco.getBalance(target.getUniqueId());
            String msg = plugin.getConfigFile().getString("economy.messages.balance-other", "&7Saldo di %player%: &a%symbol%%amount%")
                    .replace("%player%", target.getName()).replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", bal));
            Messages.send(sender, msg);
        } else {
            if (!(sender instanceof Player player)) { Messages.send(sender, "&cSpecifica un giocatore!"); return; }
            double bal = eco.getBalance(player.getUniqueId());
            String msg = plugin.getConfigFile().getString("economy.messages.balance", "&7Saldo: &a%symbol%%amount%")
                    .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", bal));
            Messages.send(sender, msg);
        }
    }

    // /pay <player> <amount>
    @Command(name = "pay", description = "Paga un giocatore")
    public void pay(@Sender Player sender, @Named("giocatore") Player target, @Range(min = 1) @Named("importo") double amount) {
        if (target.equals(sender)) { Messages.send(sender, "&cNon puoi pagare te stesso!"); return; }

        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();

        if (!eco.removeBalance(sender.getUniqueId(), amount)) {
            Messages.send(sender, plugin.getConfigFile().getString("economy.messages.pay-insufficient", "&cSaldo insufficiente!"));
            return;
        }
        eco.addBalance(target.getUniqueId(), amount);

        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.pay-sent", "&7Inviati %symbol%%amount% a %player%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", target.getName()));
        Messages.send(target, plugin.getConfigFile().getString("economy.messages.pay-received", "&7Ricevuti %symbol%%amount% da %player%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", sender.getName()));
    }

    // /baltop
    @Command(name = "baltop", description = "Classifica saldi")
    public void baltop(@Sender CommandSender sender) {
        EconomyManager eco = EconomyManager.getInstance();
        if (eco == null) { Messages.send(sender, "&cEconomy non disponibile."); return; }

        int topSize = plugin.getConfigFile().getInt("economy.baltop-size", 10);
        String symbol = eco.getCurrencySymbol();

        Messages.sendRaw(sender, plugin.getConfigFile().getString("economy.messages.baltop-header", "&8&m----------&8[ &bBaltop &8]&8&m----------"));
        List<Map.Entry<UUID, Double>> top = eco.getTopBalances(topSize);
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<UUID, Double> entry = top.get(i);
            OfflinePlayer p = Bukkit.getOfflinePlayer(entry.getKey());
            String name = p.getName() != null ? p.getName() : entry.getKey().toString().substring(0, 8);
            Messages.sendRaw(sender, plugin.getConfigFile().getString("economy.messages.baltop-entry", "&7#%position% &b%player% &7- &a%symbol%%amount%")
                    .replace("%position%", String.valueOf(i + 1)).replace("%player%", name)
                    .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", entry.getValue())));
        }
        if (top.isEmpty()) Messages.sendRaw(sender, "&7Nessun dato disponibile.");
        Messages.sendRaw(sender, plugin.getConfigFile().getString("economy.messages.baltop-footer", "&8&m---------------------------------"));
    }

    // /eco give|take|set
    @Command(name = "eco", aliases = {"economy"}, description = "Gestione economia")
    @Permission("hiru.admin")
    public void ecoRoot(@Sender CommandSender sender) {
        Messages.send(sender, "&cUso: /eco <give|take|set> <giocatore> <importo>");
    }

    @Subcommand(name = "give", description = "Dai soldi")
    @Permission("hiru.admin")
    public void ecoGive(@Sender CommandSender sender, @Named("giocatore") OfflinePlayer target, @Range(min = 1) @Named("importo") double amount) {
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.addBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-give", "&7Dati %symbol%%amount% a %player%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", name));
    }

    @Subcommand(name = "take", description = "Togli soldi")
    @Permission("hiru.admin")
    public void ecoTake(@Sender CommandSender sender, @Named("giocatore") OfflinePlayer target, @Range(min = 1) @Named("importo") double amount) {
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.removeBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-take", "&7Rimossi %symbol%%amount% da %player%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", name));
    }

    @Subcommand(name = "set", description = "Imposta saldo")
    @Permission("hiru.admin")
    public void ecoSet(@Sender CommandSender sender, @Named("giocatore") OfflinePlayer target, @Range(min = 0) @Named("importo") double amount) {
        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();
        eco.setBalance(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.eco-set", "&7Saldo di %player% impostato a %symbol%%amount%.")
                .replace("%symbol%", symbol).replace("%amount%", String.format("%.2f", amount)).replace("%player%", name));
    }
}
