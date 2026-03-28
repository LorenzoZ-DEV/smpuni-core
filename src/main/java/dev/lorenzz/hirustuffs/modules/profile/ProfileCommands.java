package dev.lorenzz.hirustuffs.modules.profile;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.managers.StatsManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class ProfileCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "profile", description = "View a player's profile")
    @Permission("hiru.profile")
    public void profile(@Sender Player viewer, @Optional @Named("player") Player target) {
        Player check = target != null ? target : viewer;
        openProfile(viewer, check);
    }

    private void openProfile(Player viewer, Player target) {
        String title = plugin.getConfigFile().getString("profile.menu-title", "&8Profile of %player%")
                .replace("%player%", target.getName());

        GUI.Builder builder = GUI.builder(title, 5).border(Material.BLACK_STAINED_GLASS_PANE);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(target);
            meta.displayName(Messages.colorComponent("&b" + target.getName()));
            head.setItemMeta(meta);
        }
        builder.item(13, head);

        StatsManager stats = StatsManager.getInstance();
        int kills = stats != null ? stats.getKills(target.getUniqueId()) : 0;
        int deaths = stats != null ? stats.getDeaths(target.getUniqueId()) : 0;
        String playtime = stats != null ? stats.getFormattedPlaytime(target.getUniqueId()) : "0h 0m";

        builder.item(20, Material.DIAMOND_SWORD, "&cStats",
                List.of("&7Kills: &c" + kills, "&7Deaths: &c" + deaths,
                        "&7K/D: &c" + (deaths == 0 ? kills : String.format("%.2f", (double) kills / deaths))));
        builder.item(22, Material.CLOCK, "&ePlaytime", List.of("&7Time played: &e" + playtime));

        EconomyManager eco = EconomyManager.getInstance();
        double balance = eco != null ? eco.getBalance(target.getUniqueId()) : 0;
        builder.item(24, Material.GOLD_INGOT, "&6Economy",
                List.of("&7Balance: &6" + (eco != null ? eco.getCurrencySymbol() : "$") + String.format("%.2f", balance)));

        ShardManager sm = ShardManager.getInstance();
        int shards = sm != null ? sm.getShards(target.getUniqueId()) : 0;
        builder.item(30, Material.AMETHYST_SHARD, "&dShards", List.of("&7Shards: &d" + shards));

        if (viewer.hasPermission("hiru.admin") && !viewer.equals(target)) {
            builder.item(32, Material.CHEST, "&aInventory",
                    List.of("&7Click to view inventory"),
                    p -> p.openInventory(target.getInventory()));
        }

        builder.open(viewer);
    }
}
