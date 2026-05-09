package dev.lorenzz.solum.modules.shards;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.ShardManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@CommandAlias("shardadmin|shardsadmin|shadmin")
@CommandPermission("solum.admin")
@Description("Shard admin management")
public class ShardAdminCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    @HelpCommand
    public void shardAdminRoot(CommandSender sender) {
        Messages.sendRaw(sender, "&8&m---------&8[ &d&lShard Admin &8]&8&m---------");
        Messages.sendRaw(sender, "&d/shardadmin give <player> <amount> &7- Give shards");
        Messages.sendRaw(sender, "&d/shardadmin take <player> <amount> &7- Take shards");
        Messages.sendRaw(sender, "&d/shardadmin set <player> <amount> &7- Set shards");
        Messages.sendRaw(sender, "&d/shardadmin check <player> &7- Check player shards");
        Messages.sendRaw(sender, "&d/shardadmin reset <player> &7- Reset player shards to 0");
        Messages.sendRaw(sender, "&8&m-----------------------------------");
    }

    @Subcommand("give")
    @Description("Give shards to a player")
    public void shardGive(CommandSender sender, OfflinePlayer target, int amount) {
        if (amount < 1) { Messages.send(sender, "&cAmount must be at least 1."); return; }
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        sm.addShards(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, "&7Given &d" + Messages.formatNumber(amount) + " Shards &7to &b" + name + "&7. " +
                "New balance: &d" + Messages.formatNumber(sm.getShards(target.getUniqueId())) + " Shards");
    }

    @Subcommand("take")
    @Description("Take shards from a player")
    public void shardTake(CommandSender sender, OfflinePlayer target, int amount) {
        if (amount < 1) { Messages.send(sender, "&cAmount must be at least 1."); return; }
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        int current = sm.getShards(target.getUniqueId());
        if (current < amount) {
            Messages.send(sender, "&c" + name + " only has &d" + Messages.formatNumber(current) + " Shards&c!");
            return;
        }
        sm.removeShards(target.getUniqueId(), amount);
        Messages.send(sender, "&7Removed &d" + Messages.formatNumber(amount) + " Shards &7from &b" + name + "&7. " +
                "New balance: &d" + Messages.formatNumber(sm.getShards(target.getUniqueId())) + " Shards");
    }

    @Subcommand("set")
    @Description("Set a player's shards")
    public void shardSet(CommandSender sender, OfflinePlayer target, int amount) {
        if (amount < 0) { Messages.send(sender, "&cAmount must be at least 0."); return; }
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        sm.setShards(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, "&7Shards of &b" + name + " &7set to &d" + Messages.formatNumber(amount) + " Shards&7.");
    }

    @Subcommand("check")
    @Description("Check a player's shards")
    public void shardCheck(CommandSender sender, OfflinePlayer target) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        int amount = sm.getShards(target.getUniqueId());
        Messages.send(sender, "&7Shards of &b" + name + "&7: &d" + Messages.formatNumber(amount));
    }

    @Subcommand("reset")
    @Description("Reset a player's shards to 0")
    public void shardReset(CommandSender sender, OfflinePlayer target) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        sm.setShards(target.getUniqueId(), 0);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, "&7Shards of &b" + name + " &7have been reset to &d0&7.");
    }
}
