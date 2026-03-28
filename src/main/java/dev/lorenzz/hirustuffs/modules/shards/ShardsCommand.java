package dev.lorenzz.hirustuffs.modules.shards;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShardsCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "shards", description = "Check your shards")
    public void shards(@Sender CommandSender sender, @Optional @Named("player") Player target) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards not available."); return; }

        if (target != null) {
            int amount = sm.getShards(target.getUniqueId());
            Messages.send(sender, "&7Shards of &b" + target.getName() + "&7: &d" + amount);
        } else {
            if (!(sender instanceof Player player)) { Messages.send(sender, "&cSpecify a player!"); return; }
            int amount = sm.getShards(player.getUniqueId());
            String msg = plugin.getConfigFile().getString("shards.messages.balance", "&7Your Shards: &d%amount%")
                    .replace("%amount%", String.valueOf(amount));
            Messages.send(sender, msg);
        }
    }
}
