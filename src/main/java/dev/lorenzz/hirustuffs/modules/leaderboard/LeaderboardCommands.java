package dev.lorenzz.hirustuffs.modules.leaderboard;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.managers.StatsManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "leaderboard", aliases = {"lb"}, description = "Open leaderboards", async = false)
    @Permission("hiru.leaderboard")
    public void leaderboard(@Sender Player player) {
        openCategoriesGUI(player);
    }

    private void openCategoriesGUI(Player player) {
        GUI.Builder builder = GUI.builder("&#333333&lLeaderboards", 3).border(Material.BLACK_STAINED_GLASS_PANE);

        builder.item(10, Material.GOLD_INGOT, "&#FFD700&lMoney", List.of("&#8C8C8CTop 10 richest players"),
                p -> openMoneyLB(p));
        builder.item(11, Material.DIAMOND_SWORD, "&#FF4444&lKills", List.of("&#8C8C8CTop 10 killers"),
                p -> openStatLB(p, "Kills", "kills"));
        builder.item(12, Material.SKELETON_SKULL, "&#CE93D8&lDeaths", List.of("&#8C8C8CTop 10 deaths"),
                p -> openStatLB(p, "Deaths", "deaths"));
        builder.item(14, Material.AMETHYST_SHARD, "&#4FC3F7&lShards", List.of("&#8C8C8CTop 10 shard holders"),
                p -> openShardsLB(p));
        builder.item(16, Material.CLOCK, "&#FF9800&lPlaytime", List.of("&#8C8C8CTop 10 playtime"),
                p -> openPlaytimeLB(p));

        builder.open(player);
    }

    private void openMoneyLB(Player player) {
        EconomyManager eco = EconomyManager.getInstance();
        if (eco == null) { Messages.send(player, "&cEconomy not available."); return; }


        List<Map.Entry<UUID, Double>> top = eco.getTopBalances(10);
        GUI.Builder builder = GUI.builder("&#333333&lTop Money", 4).border(Material.GOLD_BLOCK);

        int slot = 10;
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : top) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "???";
            String rankColor = getRankColor(rank);

            ItemStack head = createHead(op, rankColor + "&l#" + rank + " " + name,
                    List.of("&#8C8C8CBalance: &#FFD700$" + String.format("%.2f", entry.getValue())));
            builder.item(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2;
            rank++;
        }

        builder.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openCategoriesGUI(p));
        builder.open(player);
    }

    private void openStatLB(Player player, String title, String key) {
        StatsManager stats = StatsManager.getInstance();
        if (stats == null) { Messages.send(player, "&cStats not available."); return; }

        List<Map.Entry<UUID, Integer>> top = stats.getTopStat(key, 10);
        String titleColor = key.equals("kills") ? "&#FF4444" : "&#CE93D8";
        GUI.Builder builder = GUI.builder("&#333333&lTop " + title, 4)
                .border(key.equals("kills") ? Material.REDSTONE_BLOCK : Material.PURPLE_STAINED_GLASS_PANE);

        int slot = 10;
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "???";
            String rankColor = getRankColor(rank);

            ItemStack head = createHead(op, rankColor + "&l#" + rank + " " + name,
                    List.of("&#8C8C8C" + title + ": " + titleColor + entry.getValue()));
            builder.item(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2;
            rank++;
        }

        builder.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openCategoriesGUI(p));
        builder.open(player);
    }

    private void openShardsLB(Player player) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) { Messages.send(player, "&cShards not available."); return; }

        List<Map.Entry<UUID, Integer>> top = sm.getTopShards(10);
        GUI.Builder builder = GUI.builder("&#333333&lTop Shards", 4).border(Material.AMETHYST_BLOCK);

        int slot = 10;
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "???";
            String rankColor = getRankColor(rank);

            ItemStack head = createHead(op, rankColor + "&l#" + rank + " " + name,
                    List.of("&#8C8C8CShards: &#4FC3F7" + entry.getValue()));
            builder.item(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2;
            rank++;
        }

        builder.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openCategoriesGUI(p));
        builder.open(player);
    }

    private void openPlaytimeLB(Player player) {
        StatsManager stats = StatsManager.getInstance();
        if (stats == null) { Messages.send(player, "&cStats not available."); return; }

        List<Map.Entry<UUID, Integer>> top = stats.getTopPlaytime(10);
        GUI.Builder builder = GUI.builder("&#333333&lTop Playtime", 4).border(Material.ORANGE_STAINED_GLASS_PANE);

        int slot = 10;
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : top) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "???";
            String rankColor = getRankColor(rank);
            int seconds = entry.getValue();
            String time = (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";

            ItemStack head = createHead(op, rankColor + "&l#" + rank + " " + name,
                    List.of("&#8C8C8CPlaytime: &#FF9800" + time));
            builder.item(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2;
            rank++;
        }

        builder.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openCategoriesGUI(p));
        builder.open(player);
    }

    private ItemStack createHead(OfflinePlayer op, String displayName, List<String> loreLines) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(op);
            meta.displayName(Messages.colorComponent(displayName));
            meta.lore(loreLines.stream().map(Messages::colorComponent).toList());
            head.setItemMeta(meta);
        }
        return head;
    }

    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "&#FFD700";
            case 2 -> "&#C0C0C0";
            case 3 -> "&#CD7F32";
            default -> "&#E0E0E0";
        };
    }
}
