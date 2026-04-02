package dev.lorenzz.hirustuffs.placeholders;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.managers.StatsManager;
import dev.lorenzz.hirustuffs.managers.BountyManager;
import dev.lorenzz.hirustuffs.managers.TeamsManager;
import dev.lorenzz.hirustuffs.managers.HomeManager;
import dev.lorenzz.hirustuffs.managers.PlaytimeRewardsManager;
import dev.lorenzz.hirustuffs.modules.easter.EasterModule;
import dev.lorenzz.hirustuffs.modules.keyall.KeyAllCommands;
import dev.lorenzz.hirustuffs.utils.Messages;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HiruExpansion extends PlaceholderExpansion {

    private final Hiru plugin;

    public HiruExpansion(Hiru plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hiru";
    }

    @Override
    public @NotNull String getAuthor() {
        return "lorenzz";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {

        // ── Leaderboard placeholders don't need a player context (holograms, signs, etc.) ──
        if (params.toLowerCase().startsWith("top_")) {
            return handleTopPlaceholder(params.toLowerCase());
        }

        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "money" -> {
                EconomyManager eco = EconomyManager.getInstance();
                if (eco == null) return "0";
                return Messages.formatNumber(eco.getBalance(player.getUniqueId()));
            }
            case "kills" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return "0";
                return Messages.formatNumber(stats.getKills(player.getUniqueId()));
            }
            case "deaths" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return "0";
                return Messages.formatNumber(stats.getDeaths(player.getUniqueId()));
            }
            case "playtime" -> {
                PlaytimeRewardsManager prm = PlaytimeRewardsManager.getInstance();
                if (prm != null) {
                    return prm.getFormattedActivePlaytime(player.getUniqueId());
                }
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return "0h 0m";
                return stats.getFormattedPlaytime(player.getUniqueId());
            }
            case "shards" -> {
                ShardManager sm = ShardManager.getInstance();
                if (sm == null) return "0";
                return Messages.formatNumber(sm.getShards(player.getUniqueId()));
            }
            case "rank" -> {
                return getRank(player);
            }
            case "kdr" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return "0.00";
                int kills = stats.getKills(player.getUniqueId());
                int deaths = stats.getDeaths(player.getUniqueId());
                if (deaths == 0) return String.valueOf(kills);
                return String.format("%.2f", (double) kills / deaths);
            }
            case "bounty" -> {
                BountyManager bm = BountyManager.getInstance();
                if (bm == null) return "0";
                BountyManager.Bounty b = bm.getBounty(player.getUniqueId());
                return b != null ? Messages.formatNumber(b.amount()) : "0";
            }
            case "team" -> {
                TeamsManager tm = TeamsManager.getInstance();
                if (tm == null) return "None";
                TeamsManager.Team team = tm.getPlayerTeam(player.getUniqueId());
                return team != null ? team.getName() : "None";
            }
            case "team_name" -> {
                TeamsManager tm = TeamsManager.getInstance();
                if (tm == null) return "";
                TeamsManager.Team team = tm.getPlayerTeam(player.getUniqueId());
                return team != null ? team.getName() : "";
            }
            case "team_role" -> {
                TeamsManager tm = TeamsManager.getInstance();
                if (tm == null) return "";
                TeamsManager.Team team = tm.getPlayerTeam(player.getUniqueId());
                if (team == null) return "";
                TeamsManager.Role role = team.getRole(player.getUniqueId());
                return role != null ? role.name() : "";
            }
            case "team_members" -> {
                TeamsManager tm = TeamsManager.getInstance();
                if (tm == null) return "0";
                TeamsManager.Team team = tm.getPlayerTeam(player.getUniqueId());
                return team != null ? String.valueOf(team.getMembers().size()) : "0";
            }
            case "team_leader" -> {
                TeamsManager tm = TeamsManager.getInstance();
                if (tm == null) return "";
                TeamsManager.Team team = tm.getPlayerTeam(player.getUniqueId());
                if (team == null) return "";
                return getPlayerName(team.getLeader());
            }
            case "has_team" -> {
                TeamsManager tm = TeamsManager.getInstance();
                if (tm == null) return "false";
                return String.valueOf(tm.hasTeam(player.getUniqueId()));
            }
            case "homes" -> {
                HomeManager hm = HomeManager.getInstance();
                if (hm == null) return "0";
                return String.valueOf(hm.getHomeCount(player.getUniqueId()));
            }
            case "keyall" -> {
                return KeyAllCommands.getTimeUntilNextKeyAll();
            }
            case "keyall_type" -> {
                String type = KeyAllCommands.getLastKeyAllType();
                return type.isEmpty() ? "None" : type;
            }
            case "keyall_amount" -> {
                int amount = KeyAllCommands.getLastKeyAllAmount();
                return amount == 0 ? "0" : String.valueOf(amount);
            }
            case "easter_eggs" -> {
                EasterModule easterModule = plugin.getModuleManager().getModule("easter");
                if (easterModule == null || easterModule.getEasterManager() == null) return "0";
                return String.valueOf(easterModule.getEasterManager().getPlayerEggs(player.getUniqueId()));
            }
            case "easter_active" -> {
                EasterModule easterModule = plugin.getModuleManager().getModule("easter");
                if (easterModule == null || easterModule.getEasterManager() == null) return "false";
                return String.valueOf(easterModule.getEasterManager().isActive());
            }
            case "easter_remaining" -> {
                EasterModule easterModule = plugin.getModuleManager().getModule("easter");
                if (easterModule == null || easterModule.getEasterManager() == null) return "0";
                return String.valueOf(easterModule.getEasterManager().getRemainingEggs());
            }
            case "easter_total" -> {
                EasterModule easterModule = plugin.getModuleManager().getModule("easter");
                if (easterModule == null || easterModule.getEasterManager() == null) return "0";
                return String.valueOf(easterModule.getEasterManager().getTotalCollected());
            }
            case "easter_time" -> {
                EasterModule easterModule = plugin.getModuleManager().getModule("easter");
                if (easterModule == null || easterModule.getEasterManager() == null) return "N/A";
                return easterModule.getEasterManager().getTimeRemaining();
            }
        }

        return null;
    }

    /**
     * Handles leaderboard placeholders.
     * Format: top_<type>_<position>_<name|value>
     * Examples:
     *   %hiru_top_kills_1_name%   → top killer name
     *   %hiru_top_kills_1_value%  → top killer kills count
     *   %hiru_top_money_3_name%   → #3 richest player name
     *   %hiru_top_money_3_value%  → #3 richest balance
     *   %hiru_top_deaths_5_name%  → #5 most deaths player
     *   %hiru_top_playtime_1_value% → top playtime formatted
     *   %hiru_top_shards_2_name%  → #2 shards holder
     */
    private String handleTopPlaceholder(String params) {
        // top_kills_1_name → ["top", "kills", "1", "name"]
        String[] parts = params.split("_");
        if (parts.length < 4) return "";

        String type = parts[1];
        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return "";
        }
        if (position < 1 || position > 100) return "";

        String field = parts[3]; // "name" or "value"   wda
        boolean wantName = field.equals("name");
        boolean wantValue = field.equals("value");
        if (!wantName && !wantValue) return "";

        switch (type) {
            case "kills", "deaths" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return wantName ? "---" : "0";
                List<Map.Entry<UUID, Integer>> top = stats.getTopStat(type, position);
                if (top.size() < position) return wantName ? "---" : "0";
                Map.Entry<UUID, Integer> entry = top.get(position - 1);
                if (wantName) return getPlayerName(entry.getKey());
                return Messages.formatNumber(entry.getValue());
            }
            case "playtime" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return wantName ? "---" : "0h 0m";
                List<Map.Entry<UUID, Integer>> top = stats.getTopStat("playtime", position);
                if (top.size() < position) return wantName ? "---" : "0h 0m";
                Map.Entry<UUID, Integer> entry = top.get(position - 1);
                if (wantName) return getPlayerName(entry.getKey());
                return stats.getFormattedPlaytime(entry.getKey());
            }
            case "money" -> {
                EconomyManager eco = EconomyManager.getInstance();
                if (eco == null) return wantName ? "---" : "0";
                List<Map.Entry<UUID, Double>> top = eco.getTopBalances(position);
                if (top.size() < position) return wantName ? "---" : "0.00";
                Map.Entry<UUID, Double> entry = top.get(position - 1);
                if (wantName) return getPlayerName(entry.getKey());
                return Messages.formatNumber(entry.getValue());
            }
            case "shards" -> {
                ShardManager sm = ShardManager.getInstance();
                if (sm == null) return wantName ? "---" : "0";
                List<Map.Entry<UUID, Integer>> top = sm.getTopShards(position);
                if (top.size() < position) return wantName ? "---" : "0";
                Map.Entry<UUID, Integer> entry = top.get(position - 1);
                if (wantName) return getPlayerName(entry.getKey());
                return Messages.formatNumber(entry.getValue());
            }
            default -> { return ""; }
        }
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
        return p.getName() != null ? p.getName() : "???";
    }


    private String getRank(OfflinePlayer player) {
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) return "Default";
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider == null) return "Default";
            LuckPerms lp = provider.getProvider();
            User user = lp.getUserManager().getUser(player.getUniqueId());
            if (user == null) return "Default";
            return user.getPrimaryGroup();
        } catch (Exception e) {
            return "Default";
        }
    }
}
