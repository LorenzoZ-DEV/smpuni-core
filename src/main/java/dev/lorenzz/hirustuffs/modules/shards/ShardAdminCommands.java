package dev.lorenzz.hirustuffs.modules.shards;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class ShardAdminCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "shardadmin", aliases = {"shardsadmin", "shadmin"}, description = "Shard admin management")
    @Permission("hiru.admin")
    public void shardAdminRoot(@Sender CommandSender sender) {
        Messages.sendRaw(sender, "&8&m---------&8[ &d&lShard Admin &8]&8&m---------");
        Messages.sendRaw(sender, "&d/shardadmin give <player> <amount> &7- Give shards");
        Messages.sendRaw(sender, "&d/shardadmin take <player> <amount> &7- Take shards");
        Messages.sendRaw(sender, "&d/shardadmin set <player> <amount> &7- Set shards");
        Messages.sendRaw(sender, "&d/shardadmin check <player> &7- Check player shards");
        Messages.sendRaw(sender, "&d/shardadmin reset <player> &7- Reset player shards to 0");
        Messages.sendRaw(sender, "&8&m-----------------------------------");
    }

    @Subcommand(name = "give", description = "Give shards to a player")
    @Permission("hiru.admin")
    public void shardGive(@Sender CommandSender sender, @Named("player") OfflinePlayer target, @Range(min = 1) @Named("amount") int amount) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        sm.addShards(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, "&7Given &d" + Messages.formatNumber(amount) + " Shards &7to &b" + name + "&7. " +
                "New balance: &d" + Messages.formatNumber(sm.getShards(target.getUniqueId())) + " Shards");
    }

    @Subcommand(name = "take", description = "Take shards from a player")
    @Permission("hiru.admin")
    public void shardTake(@Sender CommandSender sender, @Named("player") OfflinePlayer target, @Range(min = 1) @Named("amount") int amount) {
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

    @Subcommand(name = "set", description = "Set a player's shards")
    @Permission("hiru.admin")
    public void shardSet(@Sender CommandSender sender, @Named("player") OfflinePlayer target, @Range(min = 0) @Named("amount") int amount) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        sm.setShards(target.getUniqueId(), amount);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, "&7Shards of &b" + name + " &7set to &d" + Messages.formatNumber(amount) + " Shards&7.");
    }

    @Subcommand(name = "check", description = "Check a player's shards")
    @Permission("hiru.admin")
    public void shardCheck(@Sender CommandSender sender, @Named("player") OfflinePlayer target) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        int amount = sm.getShards(target.getUniqueId());
        Messages.send(sender, "&7Shards of &b" + name + "&7: &d" + Messages.formatNumber(amount));
    }

    @Subcommand(name = "reset", description = "Reset a player's shards to 0")
    @Permission("hiru.admin")
    public void shardReset(@Sender CommandSender sender, @Named("player") OfflinePlayer target) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards system not available."); return; }

        sm.setShards(target.getUniqueId(), 0);
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        Messages.send(sender, "&7Shards of &b" + name + " &7have been reset to &d0&7.");
    }
}
