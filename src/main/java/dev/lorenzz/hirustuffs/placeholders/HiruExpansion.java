package dev.lorenzz.hirustuffs.placeholders;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.managers.StatsManager;
import dev.lorenzz.hirustuffs.managers.BountyManager;
import dev.lorenzz.hirustuffs.managers.TeamsManager;
import dev.lorenzz.hirustuffs.managers.HomeManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

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
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "money" -> {
                EconomyManager eco = EconomyManager.getInstance();
                if (eco == null) return "0";
                return String.format("%.2f", eco.getBalance(player.getUniqueId()));
            }
            case "kills" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return "0";
                return String.valueOf(stats.getKills(player.getUniqueId()));
            }
            case "deaths" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return "0";
                return String.valueOf(stats.getDeaths(player.getUniqueId()));
            }
            case "playtime" -> {
                StatsManager stats = StatsManager.getInstance();
                if (stats == null) return "0h 0m";
                return stats.getFormattedPlaytime(player.getUniqueId());
            }
            case "shards" -> {
                ShardManager sm = ShardManager.getInstance();
                if (sm == null) return "0";
                return String.valueOf(sm.getShards(player.getUniqueId()));
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
                return b != null ? String.valueOf(b.amount()) : "0";
            }
            case "team" -> {
                TeamsManager tm = TeamsManager.getInstance();
                if (tm == null) return "None";
                TeamsManager.Team team = tm.getPlayerTeam(player.getUniqueId());
                return team != null ? team.getName() : "None";
            }
            case "homes" -> {
                HomeManager hm = HomeManager.getInstance();
                if (hm == null) return "0";
                return String.valueOf(hm.getHomeCount(player.getUniqueId()));
            }
        }

        return null;
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
