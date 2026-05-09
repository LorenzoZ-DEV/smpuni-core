package dev.lorenzz.solum.modules.bounty;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.BountyManager;
import dev.lorenzz.solum.managers.BountyManager.Bounty;
import dev.lorenzz.solum.managers.ShardManager;
import dev.lorenzz.solum.utils.GUI;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

@CommandAlias("bounty")
@CommandPermission("solum.bounty")
@Description("Bounty system")
public class BountyCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    @Default
    public void bountyRoot(Player player, @Optional Player target, @Optional Integer amount) {
        BountyManager bm = BountyManager.getInstance();
        if (bm == null) { Messages.send(player, "&cBounty system not available."); return; }

        int amt = (amount == null) ? 0 : amount;
        if (target == null || amt <= 0) {
            BountyHelper.openBountyGUI(player);
            return;
        }

        if (target.equals(player)) {
            Messages.send(player, "&#FF4444You can't place a bounty on yourself!");
            return;
        }

        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(player, "&cShards not available."); return; }

        int min = plugin.getConfigFile().getInt("bounty.min-amount", 50);
        if (amt < min) {
            Messages.send(player, "&#FF4444Minimum bounty: &#FFD700" + min + " Shards");
            return;
        }

        if (!sm.removeShards(player.getUniqueId(), amt)) {
            Messages.send(player, "&#FF4444Not enough Shards!");
            return;
        }

        bm.placeBounty(player.getUniqueId(), target.getUniqueId(), amt);

        Messages.send(player, "&#55FF55Bounty of &#FFD700" + amt + " Shards &#55FF55placed on &#FF6B6B" + target.getName());
        Messages.send(target, "&#FF4444\u26A0 &#FFD700" + player.getName() + " &#E0E0E0placed a bounty of &#FFD700" + amt + " Shards &#E0E0E0on you!");

        Bukkit.broadcast(Messages.colorComponent(
                "&#FF4444&l\u26A0 BOUNTY &#8C8C8C| &#FFD700" + player.getName() +
                " &#E0E0E0placed &#FFD700" + amt + " Shards &#E0E0E0on &#FF6B6B" + target.getName()));
    }
}

@CommandAlias("bounties")
@CommandPermission("solum.bounty")
@Description("View active bounties")
class BountiesCommand extends BaseCommand {
    @Default
    public void bounties(Player player) {
        BountyHelper.openBountyGUI(player);
    }
}

class BountyHelper {

    static void openBountyGUI(Player player) {
        BountyManager bm = BountyManager.getInstance();
        if (bm == null) { Messages.send(player, "&cBounty system not available."); return; }
        List<Bounty> bounties = bm.getAllBounties();

        int rows = Math.min(Math.max((bounties.size() / 7) + 3, 3), 6);
        GUI.Builder builder = GUI.builder("&#333333&lActive Bounties &#FF4444(" + bounties.size() + ")", rows)
                .border(Material.RED_STAINED_GLASS_PANE);

        int slot = 10;
        int maxSlots = (rows * 9) - 9;
        for (Bounty bounty : bounties) {
            if (slot >= maxSlots) break;
            String targetName = Bukkit.getOfflinePlayer(bounty.target()).getName();
            if (targetName == null) targetName = bounty.target().toString().substring(0, 8);
            String setterName = Bukkit.getOfflinePlayer(bounty.setter()).getName();
            if (setterName == null) setterName = "Unknown";

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(bounty.target()));
                meta.displayName(Messages.colorComponent("&#FF6B6B" + targetName + " &#8C8C8C| &#FFD700Bounty: $" + formatNumber(bounty.amount())));
                meta.lore(List.of(
                        Messages.colorComponent(""),
                        Messages.colorComponent("&#8C8C8CReward: &#FFD700" + bounty.amount() + " Shards"),
                        Messages.colorComponent("&#8C8C8CSet by: &#E0E0E0" + setterName),
                        Messages.colorComponent(""),
                        Messages.colorComponent("&#FF4444Kill this player to claim!")
                ));
                head.setItemMeta(meta);
            }
            builder.item(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (bounties.isEmpty()) {
            builder.item(13, Material.BARRIER, "&#8C8C8CNo active bounties",
                    List.of("&#8C8C8CUse /bounty <player> <amount>"));
        }

        builder.open(player);
    }

    private static String formatNumber(int value) {
        if (value >= 1_000_000) return String.format("%.2fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.2fK", value / 1_000.0);
        return String.valueOf(value);
    }
}

