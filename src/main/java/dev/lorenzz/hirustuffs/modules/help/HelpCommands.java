package dev.lorenzz.hirustuffs.modules.help;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class HelpCommands {

    @Command(name = "help", aliases = {"h", "?"}, description = "Server help menu", async = false)
    public void help(@Sender Player player) {
        openMainHelp(player);
    }

    // ───────────────────────────────────────────
    //  MAIN MENU — Category selector
    // ───────────────────────────────────────────
    private void openMainHelp(Player player) {
        GUI.Builder b = GUI.builder("&#1A1A2E&lServer Guide", 5)
                .fill(Material.GRAY_STAINED_GLASS_PANE)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        // Header — server title
        b.item(4, Material.NETHER_STAR, "&#0053FF&lSMPUNI &#8C8C8C- &#FF4444&lBETA",
                List.of(
                        "",
                        "&#E0E0E0Welcome to the server!",
                        "&#8C8C8CClick a category below to learn more.",
                        ""
                ));

        // Row 2 — main categories
        b.item(19, Material.GOLD_INGOT, "&#FFD700&l$ Economy",
                List.of("&#8C8C8CEarn money, trade & shop", "", "&#FFD700&l➥ Click to view"),
                p -> openEconomyHelp(p));

        b.item(20, Material.DIAMOND_SWORD, "&#FF4444&l⚔ Combat & Bounty",
                List.of("&#8C8C8CPvP, kills & bounties", "", "&#FF4444&l➥ Click to view"),
                p -> openCombatHelp(p));

        b.item(21, Material.ENDER_PEARL, "&#4FC3F7&l⟐ Teleportation",
                List.of("&#8C8C8CTPA, homes, spawn & RTP", "", "&#4FC3F7&l➥ Click to view"),
                p -> openTeleportHelp(p));

        b.item(23, Material.SHIELD, "&#55FF55&l⛊ Teams",
                List.of("&#8C8C8CCreate teams, invite friends", "", "&#55FF55&l➥ Click to view"),
                p -> openTeamsHelp(p));

        b.item(24, Material.AMETHYST_SHARD, "&#CE93D8&l✦ Shards & Worth",
                List.of("&#8C8C8CEarn shards, item values", "", "&#CE93D8&l➥ Click to view"),
                p -> openShardsHelp(p));

        b.item(25, Material.COMPARATOR, "&#FF9800&l⚙ Utility",
                List.of("&#8C8C8CSettings, leaderboard & more", "", "&#FF9800&l➥ Click to view"),
                p -> openUtilityHelp(p));

        // Row 3 — quick info
        b.item(31, Material.BOOK, "&#E0E0E0&lServer Rules",
                List.of("&#8C8C8CType &#E0E0E0/rules &#8C8C8Cto view the rules", "&#8C8C8CYou must accept them to play!"));

        b.open(player);
    }

    // ───────────────────────────────────────────
    //  ECONOMY
    // ───────────────────────────────────────────
    private void openEconomyHelp(Player player) {
        GUI.Builder b = GUI.builder("&#1A1A2E&lEconomy Guide", 4)
                .fill(Material.GRAY_STAINED_GLASS_PANE)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        b.item(4, Material.GOLD_INGOT, "&#FFD700&l$ Economy System",
                List.of("", "&#E0E0E0Earn money by selling items,", "&#E0E0E0trading and playing!", ""));

        b.item(10, Material.GOLD_NUGGET, "&#FFD700/balance &#8C8C8C[player]",
                List.of("&#E0E0E0Check your or another player's balance.", "&#8C8C8CAliases: &#E0E0E0/bal, /money"));

        b.item(11, Material.HOPPER, "&#FFD700/pay &#8C8C8C<player> <amount>",
                List.of("&#E0E0E0Send money to another player."));

        b.item(12, Material.GOLD_BLOCK, "&#FFD700/baltop",
                List.of("&#E0E0E0View the richest players on the server."));

        b.item(14, Material.CHEST, "&#55FF55/shop",
                List.of("&#E0E0E0Open the server shop GUI.", "&#8C8C8CBuy & sell items with money or shards."));

        b.item(15, Material.BARREL, "&#55FF55/sell",
                List.of("&#E0E0E0Open the sell menu.", "&#E0E0E0Place items inside and sell them all!"));

        b.item(16, Material.NAME_TAG, "&#55FF55/sellhand",
                List.of("&#E0E0E0Quickly sell the item in your hand."));

        b.item(19, Material.EMERALD, "&#55FF55/sellall",
                List.of("&#E0E0E0Sell every sellable item in your inventory."));

        b.item(20, Material.VILLAGER_SPAWN_EGG, "&#FFD700/billford",
                List.of("&#E0E0E0Open Billford's rotating trades.", "&#8C8C8CSpecial trades refresh every few hours!"));

        b.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openMainHelp(p));
        b.open(player);
    }

    // ───────────────────────────────────────────
    //  COMBAT & BOUNTY
    // ───────────────────────────────────────────
    private void openCombatHelp(Player player) {
        GUI.Builder b = GUI.builder("&#1A1A2E&lCombat & Bounty Guide", 4)
                .fill(Material.GRAY_STAINED_GLASS_PANE)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        b.item(4, Material.DIAMOND_SWORD, "&#FF4444&l⚔ Combat System",
                List.of("", "&#E0E0E0PvP is enabled in the wild!", "&#E0E0E0Kill players to earn stats and claim bounties.", ""));

        b.item(11, Material.SKELETON_SKULL, "&#FF4444Kill Notifications",
                List.of("&#E0E0E0When you kill a player, everyone", "&#E0E0E0sees a message with the weapon used."));

        b.item(12, Material.TNT, "&#FFD700/bounty &#8C8C8C<player> <amount>",
                List.of("&#E0E0E0Place a bounty on a player using Shards.", "&#E0E0E0Other players can kill them to claim it!"));

        b.item(13, Material.WRITABLE_BOOK, "&#FFD700/bounties",
                List.of("&#E0E0E0Open the bounty board GUI.", "&#E0E0E0See all active bounties and rewards."));

        b.item(14, Material.PLAYER_HEAD, "&#CE93D8/profile &#8C8C8C[player]",
                List.of("&#E0E0E0View a player's profile:", "&#8C8C8CKills, Deaths, Playtime, Shards & more."));

        b.item(15, Material.BARRIER, "&#FF4444/statswipe",
                List.of("&#8C8C8CAdmin only. Resets all player stats.", "&#FF4444Requires confirmation!"));

        b.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openMainHelp(p));
        b.open(player);
    }

    // ───────────────────────────────────────────
    //  TELEPORTATION
    // ───────────────────────────────────────────
    private void openTeleportHelp(Player player) {
        GUI.Builder b = GUI.builder("&#1A1A2E&lTeleportation Guide", 4)
                .fill(Material.GRAY_STAINED_GLASS_PANE)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        b.item(4, Material.ENDER_PEARL, "&#4FC3F7&l⟐ Teleportation",
                List.of("", "&#E0E0E0All teleports have a warmup.", "&#E0E0E0Don't move or it gets cancelled!", ""));

        b.item(10, Material.COMPASS, "&#4FC3F7/tpa &#8C8C8C<player>",
                List.of("&#E0E0E0Send a teleport request to a player."));

        b.item(11, Material.LIME_DYE, "&#55FF55/tpaccept",
                List.of("&#E0E0E0Accept an incoming teleport request."));

        b.item(12, Material.RED_DYE, "&#FF4444/tpdeny",
                List.of("&#E0E0E0Deny an incoming teleport request."));

        b.item(14, Material.RED_BED, "&#FFD700/home &#8C8C8C[name]",
                List.of("&#E0E0E0Teleport to your home.", "&#8C8C8COpens GUI if no name is given."));

        b.item(15, Material.OAK_SIGN, "&#FFD700/sethome &#8C8C8C[name]",
                List.of("&#E0E0E0Set a home at your current location.", "&#8C8C8CHome limit depends on your rank."));

        b.item(16, Material.TNT_MINECART, "&#FF4444/delhome &#8C8C8C<name>",
                List.of("&#E0E0E0Delete one of your homes."));

        b.item(19, Material.GRASS_BLOCK, "&#55FF55/spawn",
                List.of("&#E0E0E0Open the spawn selector menu."));

        b.item(20, Material.ENDER_EYE, "&#CE93D8/rtp",
                List.of("&#E0E0E0Random teleport to a safe location.", "&#E0E0E0Choose Overworld, Nether or End!"));

        b.item(21, Material.CHEST, "&#FFD700/homes",
                List.of("&#E0E0E0View all your homes in a GUI."));

        b.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openMainHelp(p));
        b.open(player);
    }

    // ───────────────────────────────────────────
    //  TEAMS
    // ───────────────────────────────────────────
    private void openTeamsHelp(Player player) {
        GUI.Builder b = GUI.builder("&#1A1A2E&lTeams Guide", 4)
                .fill(Material.GRAY_STAINED_GLASS_PANE)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        b.item(4, Material.SHIELD, "&#55FF55&l⛊ Teams System",
                List.of("", "&#E0E0E0Create a team, invite friends,", "&#E0E0E0set team homes and fight together!", ""));

        b.item(10, Material.WRITABLE_BOOK, "&#55FF55/team create &#8C8C8C<name>",
                List.of("&#E0E0E0Create a new team."));

        b.item(11, Material.PLAYER_HEAD, "&#55FF55/team invite &#8C8C8C<player>",
                List.of("&#E0E0E0Invite a player to your team."));

        b.item(12, Material.LIME_DYE, "&#55FF55/team accept",
                List.of("&#E0E0E0Accept a team invitation."));

        b.item(13, Material.RED_DYE, "&#FF4444/team deny",
                List.of("&#E0E0E0Deny a team invitation."));

        b.item(14, Material.OAK_DOOR, "&#FF4444/team leave",
                List.of("&#E0E0E0Leave your current team."));

        b.item(15, Material.IRON_BOOTS, "&#FF4444/team kick &#8C8C8C<player>",
                List.of("&#E0E0E0Kick a player from your team.", "&#8C8C8CRequires Leader or Moderator role."));

        b.item(16, Material.PAPER, "&#4FC3F7/team info",
                List.of("&#E0E0E0View team info and member list GUI."));

        b.item(19, Material.RED_BED, "&#FFD700/team sethome",
                List.of("&#E0E0E0Set the team home location."));

        b.item(20, Material.ENDER_PEARL, "&#FFD700/team home",
                List.of("&#E0E0E0Teleport to the team home."));

        b.item(21, Material.CAMPFIRE, "&#FF9800/team toggleff",
                List.of("&#E0E0E0Toggle friendly fire on/off.", "&#8C8C8CLeader only."));

        b.item(22, Material.TNT, "&#FF4444/team disband",
                List.of("&#E0E0E0Disband the entire team.", "&#8C8C8CLeader only. This is permanent!"));

        b.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openMainHelp(p));
        b.open(player);
    }

    // ───────────────────────────────────────────
    //  SHARDS & WORTH
    // ───────────────────────────────────────────
    private void openShardsHelp(Player player) {
        GUI.Builder b = GUI.builder("&#1A1A2E&lShards & Worth Guide", 4)
                .fill(Material.GRAY_STAINED_GLASS_PANE)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        b.item(4, Material.AMETHYST_SHARD, "&#CE93D8&l✦ Shards System",
                List.of("",
                        "&#E0E0E0Shards are a special currency.",
                        "&#E0E0E0Earn them by killing mobs, mining",
                        "&#E0E0E0ores and harvesting crops!", ""));

        b.item(11, Material.AMETHYST_CLUSTER, "&#CE93D8/shards &#8C8C8C[player]",
                List.of("&#E0E0E0Check your or another player's shard balance."));

        b.item(12, Material.EXPERIENCE_BOTTLE, "&#CE93D8How to Earn Shards",
                List.of("&#8C8C8C⬥ &#E0E0E0Kill mobs",
                        "&#8C8C8C⬥ &#E0E0E0Mine ores (Diamond, Emerald...)",
                        "&#8C8C8C⬥ &#E0E0E0Harvest crops (Wheat, Carrots...)",
                        "",
                        "&#8C8C8CAmounts are configurable by admins."));

        b.item(14, Material.SPYGLASS, "&#FFD700/worth &#8C8C8C[item]",
                List.of("&#E0E0E0Check the sell value of an item.", "&#8C8C8CHold an item or type the name."));

        b.item(15, Material.BOOK, "&#FFD700/prices",
                List.of("&#E0E0E0Open the full price list GUI.", "&#E0E0E0See all sellable items and their value."));

        b.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openMainHelp(p));
        b.open(player);
    }

    // ───────────────────────────────────────────
    //  UTILITY
    // ───────────────────────────────────────────
    private void openUtilityHelp(Player player) {
        GUI.Builder b = GUI.builder("&#1A1A2E&lUtility Guide", 4)
                .fill(Material.GRAY_STAINED_GLASS_PANE)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        b.item(4, Material.COMPARATOR, "&#FF9800&l⚙ Utility Commands",
                List.of("", "&#E0E0E0Useful commands and tools.", ""));

        b.item(10, Material.REDSTONE_TORCH, "&#FF9800/settings",
                List.of("&#E0E0E0Open your personal settings GUI.", "&#E0E0E0Toggle chat, TPA requests, notifications..."));

        b.item(11, Material.DIAMOND_BLOCK, "&#FFD700/leaderboard",
                List.of("&#E0E0E0View the top players GUI.", "&#8C8C8CMoney, Kills, Deaths, Shards, Playtime.",
                        "&#8C8C8CAlias: &#E0E0E0/lb"));

        b.item(12, Material.CLOCK, "&#4FC3F7/afk",
                List.of("&#E0E0E0Toggle AFK mode.", "&#8C8C8CYou will be kicked after inactivity!"));

        b.item(14, Material.PAINTING, "&#CE93D8/media",
                List.of("&#E0E0E0Open the media rank menu.", "&#8C8C8CRequirements & benefits."));

        b.item(15, Material.LECTERN, "&#E0E0E0/rules",
                List.of("&#E0E0E0View and accept the server rules.", "&#8C8C8CRequired on first join!"));

        b.item(16, Material.BEACON, "&#E0E0E0/info",
                List.of("&#E0E0E0View server information.", "&#8C8C8COwner, Discord, website & more."));

        b.item(19, Material.TRIPWIRE_HOOK, "&#FFD700/keyall &#8C8C8C<type> <amount>",
                List.of("&#8C8C8CAdmin only.", "&#E0E0E0Give crate keys to all online players."));

        b.item(20, Material.COMMAND_BLOCK, "&#FF4444/hiru reload",
                List.of("&#8C8C8CAdmin only.", "&#E0E0E0Reload the plugin configuration."));

        b.item(31, Material.ARROW, "&#8C8C8C← Back", List.of(), p -> openMainHelp(p));
        b.open(player);
    }
}
