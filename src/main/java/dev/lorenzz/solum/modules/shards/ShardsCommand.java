package dev.lorenzz.solum.modules.shards;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.ShardManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("shards")
@Description("Check your shards")
public class ShardsCommand extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void shards(CommandSender sender, @Optional Player target) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(sender, "&cShards not available."); return; }

        if (target != null) {
            int amount = sm.getShards(target.getUniqueId());
            Messages.send(sender, "&7Shards of &b" + target.getName() + "&7: &d" + Messages.formatNumber(amount));
        } else {
            if (!(sender instanceof Player player)) { Messages.send(sender, "&cSpecify a player!"); return; }
            int amount = sm.getShards(player.getUniqueId());
            String msg = plugin.getConfigFile().getString("shards.messages.balance", "&7Your Shards: &d%amount%")
                    .replace("%amount%", Messages.formatNumber(amount));
            Messages.send(sender, msg);
        }
    }
}
