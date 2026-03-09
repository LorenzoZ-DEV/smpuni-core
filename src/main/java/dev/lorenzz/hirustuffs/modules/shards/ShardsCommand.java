package dev.lorenzz.hirustuffs.modules.shards;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShardsCommand {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "shards", description = "Controlla i tuoi shards")
    public void shards(@Sender CommandSender sender, @Optional @Named("giocatore") Player target) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards non disponibile."); return; }

        if (target != null) {
            int amount = sm.getShards(target.getUniqueId());
            Messages.send(sender, "&7Shards di &b" + target.getName() + "&7: &d" + amount);
        } else {
            if (!(sender instanceof Player player)) { Messages.send(sender, "&cSpecifica un giocatore!"); return; }
            int amount = sm.getShards(player.getUniqueId());
            String msg = plugin.getConfigFile().getString("shards.messages.balance", "&7I tuoi Shards: &d%amount%")
                    .replace("%amount%", String.valueOf(amount));
            Messages.send(sender, msg);
        }
    }
}
