package dev.lorenzz.lxsmp.modules.economy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.EconomyManager;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@CommandAlias("balance|bal|money")
@Description("Check your balance")
public class EconomyCommands extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void balance(CommandSender sender, @Optional Player target) {
        EconomyManager eco = EconomyManager.getInstance();
        if (eco == null) { Messages.send(sender, "&cEconomy not available."); return; }

        String symbol = eco.getCurrencySymbol();
        if (target != null) {
            double bal = eco.getBalance(target.getUniqueId());
            String msg = plugin.getConfigFile().getString("economy.messages.balance-other", "&7Balance of &b%player%&7: &a%symbol%%amount%")
                    .replace("%player%", target.getName()).replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(bal));
            Messages.send(sender, msg);
        } else {
            if (!(sender instanceof Player player)) { Messages.send(sender, "&cSpecify a player!"); return; }
            double bal = eco.getBalance(player.getUniqueId());
            String msg = plugin.getConfigFile().getString("economy.messages.balance", "&7Your balance: &a%symbol%%amount%")
                    .replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(bal));
            Messages.send(sender, msg);
        }
    }
}

@CommandAlias("pay")
@Description("Pay a player")
class PayCommand extends BaseCommand {
    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void pay(Player sender, Player target, double amount) {
        if (amount < 1) { Messages.send(sender, "&cAmount must be at least 1!"); return; }
        if (target.equals(sender)) { Messages.send(sender, "&cYou can't pay yourself!"); return; }

        EconomyManager eco = EconomyManager.getInstance();
        String symbol = eco.getCurrencySymbol();

        if (!eco.removeBalance(sender.getUniqueId(), amount)) {
            Messages.send(sender, plugin.getConfigFile().getString("economy.messages.pay-insufficient", "&cInsufficient balance!"));
            return;
        }
        eco.addBalance(target.getUniqueId(), amount);

        Messages.send(sender, plugin.getConfigFile().getString("economy.messages.pay-sent", "&7Sent &a%symbol%%amount% &7to &b%player%&7.")
                .replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(amount)).replace("%player%", target.getName()));
        Messages.send(target, plugin.getConfigFile().getString("economy.messages.pay-received", "&7Received &a%symbol%%amount% &7from &b%player%&7.")
                .replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(amount)).replace("%player%", sender.getName()));
    }
}

@CommandAlias("baltop")
@Description("Balance leaderboard")
class BalTopCommand extends BaseCommand {
    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void baltop(CommandSender sender) {
        EconomyManager eco = EconomyManager.getInstance();
        if (eco == null) { Messages.send(sender, "&cEconomy not available."); return; }

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
                    .replace("%symbol%", symbol).replace("%amount%", Messages.formatNumber(entry.getValue())));
        }
        if (top.isEmpty()) Messages.sendRaw(sender, "&7No data available.");
        Messages.sendRaw(sender, plugin.getConfigFile().getString("economy.messages.baltop-footer", "&8&m---------------------------------"));
    }
}

