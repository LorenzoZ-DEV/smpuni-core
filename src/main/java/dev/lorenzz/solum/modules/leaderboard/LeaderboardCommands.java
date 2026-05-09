package dev.lorenzz.solum.modules.leaderboard;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.EconomyManager;
import dev.lorenzz.solum.managers.ShardManager;
import dev.lorenzz.solum.managers.StatsManager;
import dev.lorenzz.solum.utils.GUI;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@CommandAlias("leaderboard|lb")
@CommandPermission("solum.leaderboard")
@Description("Open leaderboards")
public class LeaderboardCommands extends BaseCommand {

    private static final int BACK_SLOT = 31;
    private static final int FIRST_ENTRY_SLOT = 10;
    private static final int LAST_COLUMN_INDEX = 8;

    private final Solum plugin = Solum.getInstance();

    @Default
    public void leaderboard(Player player) {
        openCategoriesGUI(player);
    }

    private void openCategoriesGUI(Player player) {
        GUI.Builder builder = GUI.builder("&#333333&lLeaderboards", 3)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        builder.item(10, Material.GOLD_INGOT, "&#FFD700&lMoney",
                List.of("&#8C8C8CTop 10 richest players"), this::openMoneyLB);
        builder.item(11, Material.DIAMOND_SWORD, "&#FF4444&lKills",
                List.of("&#8C8C8CTop 10 killers"), p -> openStatLB(p, "Kills", "kills"));
        builder.item(12, Material.SKELETON_SKULL, "&#CE93D8&lDeaths",
                List.of("&#8C8C8CTop 10 deaths"), p -> openStatLB(p, "Deaths", "deaths"));
        builder.item(14, Material.AMETHYST_SHARD, "&#4FC3F7&lShards",
                List.of("&#8C8C8CTop 10 shard holders"), this::openShardsLB);
        builder.item(16, Material.CLOCK, "&#FF9800&lPlaytime",
                List.of("&#8C8C8CTop 10 playtime"), this::openPlaytimeLB);

        builder.open(player);
    }

    private void openMoneyLB(Player player) {
        EconomyManager eco = EconomyManager.getInstance();
        if (eco == null) {
            Messages.send(player, "&cEconomy not available.");
            return;
        }

        GUI.Builder builder = GUI.builder("&#333333&lTop Money", 4)
                .border(Material.GOLD_BLOCK);

        populate(builder, eco.getTopBalances(10), entry ->
                List.of("&#8C8C8CBalance: &#FFD700$" + String.format("%.2f", entry.getValue())));

        appendBackButton(builder);
        builder.open(player);
    }

    private void openStatLB(Player player, String title, String key) {
        StatsManager stats = StatsManager.getInstance();
        if (stats == null) {
            Messages.send(player, "&cStats not available.");
            return;
        }

        String titleColor = key.equals("kills") ? "&#FF4444" : "&#CE93D8";
        Material borderMat = key.equals("kills")
                ? Material.REDSTONE_BLOCK
                : Material.PURPLE_STAINED_GLASS_PANE;

        GUI.Builder builder = GUI.builder("&#333333&lTop " + title, 4)
                .border(borderMat);

        populate(builder, stats.getTopStat(key, 10), entry ->
                List.of("&#8C8C8C" + title + ": " + titleColor + entry.getValue()));

        appendBackButton(builder);
        builder.open(player);
    }

    private void openShardsLB(Player player) {
        ShardManager sm = ShardManager.getInstance();
        if (sm == null) {
            Messages.send(player, "&cShards not available.");
            return;
        }

        GUI.Builder builder = GUI.builder("&#333333&lTop Shards", 4)
                .border(Material.AMETHYST_BLOCK);

        populate(builder, sm.getTopShards(10), entry ->
                List.of("&#8C8C8CShards: &#4FC3F7" + entry.getValue()));

        appendBackButton(builder);
        builder.open(player);
    }

    private void openPlaytimeLB(Player player) {
        StatsManager stats = StatsManager.getInstance();
        if (stats == null) {
            Messages.send(player, "&cStats not available.");
            return;
        }

        GUI.Builder builder = GUI.builder("&#333333&lTop Playtime", 4)
                .border(Material.ORANGE_STAINED_GLASS_PANE);

        populate(builder, stats.getTopPlaytime(10), entry ->
                List.of("&#8C8C8CPlaytime: &#FF9800" + formatPlaytime(entry.getValue())));

        appendBackButton(builder);
        builder.open(player);
    }

    private <V> void populate(GUI.Builder builder,
                              List<Map.Entry<UUID, V>> entries,
                              Function<Map.Entry<UUID, V>, List<String>> loreFactory) {
        int slot = FIRST_ENTRY_SLOT;
        int rank = 1;

        for (Map.Entry<UUID, V> entry : entries) {
            OfflinePlayer op = resolvePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "???";
            String displayName = getRankColor(rank) + "&l#" + rank + " " + name;

            builder.item(slot, createHead(op, displayName, loreFactory.apply(entry)));

            slot++;
            if (slot % 9 == LAST_COLUMN_INDEX) {
                slot += 2;
            }
            rank++;
        }
    }

    private OfflinePlayer resolvePlayer(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        return online != null ? online : Bukkit.getOfflinePlayer(uuid);
    }

    private String formatPlaytime(int seconds) {
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }

    private void appendBackButton(GUI.Builder builder) {
        builder.item(BACK_SLOT, Material.ARROW, "&#8C8C8C← Back", List.of(),
                this::openCategoriesGUI);
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

