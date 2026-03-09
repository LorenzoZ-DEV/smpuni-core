package dev.lorenzz.hirustuffs.modules.bounty;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.BountyManager;
import dev.lorenzz.hirustuffs.managers.BountyManager.Bounty;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class BountyCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "bounty", description = "Bounty system", async = false)
    @Permission("hiru.bounty")
    public void bountyRoot(@Sender Player player, @Optional @Named("player") Player target,
                           @Optional("0") @Named("amount") int amount) {
        BountyManager bm = BountyManager.getInstance();
        if (bm == null) { Messages.send(player, "&cBounty system not available."); return; }

        if (target == null || amount <= 0) {
            openBountyGUI(player);
            return;
        }

        if (target.equals(player)) {
            Messages.send(player, "&#FF4444You can't place a bounty on yourself!");
            return;
        }

        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(player, "&cShards not available."); return; }

        int min = plugin.getConfigFile().getInt("bounty.min-amount", 50);
        if (amount < min) {
            Messages.send(player, "&#FF4444Minimum bounty: &#FFD700" + min + " Shards");
            return;
        }

        if (!sm.removeShards(player.getUniqueId(), amount)) {
            Messages.send(player, "&#FF4444Not enough Shards!");
            return;
        }

        bm.placeBounty(player.getUniqueId(), target.getUniqueId(), amount);

        Messages.send(player, "&#55FF55Bounty of &#FFD700" + amount + " Shards &#55FF55placed on &#FF6B6B" + target.getName());
        Messages.send(target, "&#FF4444\u26A0 &#FFD700" + player.getName() + " &#E0E0E0placed a bounty of &#FFD700" + amount + " Shards &#E0E0E0on you!");

        Bukkit.broadcast(Messages.colorComponent(
                "&#FF4444&l\u26A0 BOUNTY &#8C8C8C| &#FFD700" + player.getName() +
                " &#E0E0E0placed &#FFD700" + amount + " Shards &#E0E0E0on &#FF6B6B" + target.getName()));
    }

    @Command(name = "bounties", description = "View active bounties", async = false)
    @Permission("hiru.bounty")
    public void bounties(@Sender Player player) {
        openBountyGUI(player);
    }

    private void openBountyGUI(Player player) {
        BountyManager bm = BountyManager.getInstance();
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

    private String formatNumber(int value) {
        if (value >= 1_000_000) return String.format("%.2fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.2fK", value / 1_000.0);
        return String.valueOf(value);
    }
}
