package dev.lorenzz.solum;

import dev.lorenzz.solum.commands.CommandRegistry;
import dev.lorenzz.solum.commands.SolumCommand;
import dev.lorenzz.solum.listeners.GlobalListener;
import dev.lorenzz.solum.managers.DatabaseManager;
import dev.lorenzz.solum.managers.Module;
import dev.lorenzz.solum.modules.afk.AFKModule;
import dev.lorenzz.solum.modules.billford.BillfordModule;
import dev.lorenzz.solum.modules.bounty.BountyModule;
import dev.lorenzz.solum.modules.clearlag.ClearLagModule;
import dev.lorenzz.solum.modules.economy.EconomyModule;
import dev.lorenzz.solum.modules.enderchest.EnderChestModule;
import dev.lorenzz.solum.modules.help.HelpModule;
import dev.lorenzz.solum.modules.homes.HomeModule;
import dev.lorenzz.solum.modules.info.InfoModule;
import dev.lorenzz.solum.modules.joinquit.JoinQuitModule;
import dev.lorenzz.solum.modules.keyall.KeyAllModule;
import dev.lorenzz.solum.modules.leaderboard.LeaderboardModule;
import dev.lorenzz.solum.modules.lunar.LunarModule;
import dev.lorenzz.solum.modules.media.MediaModule;
import dev.lorenzz.solum.modules.profile.ProfileModule;
import dev.lorenzz.solum.modules.rtp.RTPModule;
import dev.lorenzz.solum.modules.rules.RulesModule;
import dev.lorenzz.solum.modules.sell.SellModule;
import dev.lorenzz.solum.modules.scoreboard.ScoreboardModule;
import dev.lorenzz.solum.modules.auctionhouse.AuctionHouseModule;
import dev.lorenzz.solum.modules.tablist.TablistModule;
import dev.lorenzz.solum.modules.settings.SettingsModule;
import dev.lorenzz.solum.modules.shards.ShardsModule;
import dev.lorenzz.solum.modules.shop.ShopModule;
import dev.lorenzz.solum.modules.spawn.SpawnModule;
import dev.lorenzz.solum.modules.stats.StatsModule;
import dev.lorenzz.solum.modules.statswipe.StatsWipeModule;
import dev.lorenzz.solum.modules.teams.TeamsModule;
import dev.lorenzz.solum.modules.tools.AmethystToolsModule;
import dev.lorenzz.solum.modules.tpa.TPAModule;
import dev.lorenzz.solum.modules.ad.AdModule;
import dev.lorenzz.solum.modules.announcements.AnnouncementModule;
import dev.lorenzz.solum.modules.broadcast.BroadcastModule;
import dev.lorenzz.solum.modules.kits.KitModule;
import dev.lorenzz.solum.modules.report.ReportModule;
import dev.lorenzz.solum.modules.request.RequestModule;
import dev.lorenzz.solum.modules.clearchat.ClearChatModule;
import dev.lorenzz.solum.modules.skilltree.SkillTreeModule;
import dev.lorenzz.solum.modules.playtimerewards.PlaytimeRewardsModule;
import dev.lorenzz.solum.modules.worth.WorthModule;
import dev.lorenzz.solum.modules.back.BackModule;
import dev.lorenzz.solum.modules.combat.CombatModule;
import dev.lorenzz.solum.modules.warp.WarpModule;
import dev.lorenzz.solum.modules.duel.DuelModule;
import dev.lorenzz.solum.modules.changelog.ChangelogModule;
import dev.lorenzz.solum.modules.chatgames.ChatGameModule;
import dev.lorenzz.solum.modules.doublejump.DoubleJumpModule;
import dev.lorenzz.solum.modules.easter.EasterModule;
import dev.lorenzz.solum.modules.nametag.NametagModule;
import dev.lorenzz.solum.placeholders.SolumExpansion;
import dev.lorenzz.solum.utils.Config;
import dev.lorenzz.solum.utils.Messages;
import net.j4c0b3y.api.menu.MenuHandler;
import net.j4c0b3y.api.menu.utils.Item;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Function;

public final class Solum extends JavaPlugin {

    private static Solum INSTANCE;

    private Config configFile;
    private ModuleManager moduleManager;
    private CommandRegistry commandManager;
    private DatabaseManager databaseManager;
    private MenuHandler menuHandler;

    public static Solum getInstance() {
        return INSTANCE;
    }

    @Override
    public void onLoad() {
        // (no PacketEvents bootstrap needed — tablist now uses pure Bukkit API)
    }

    @Override
    public void onEnable() {
        INSTANCE = this;


        // Load config
        configFile = new Config(this, "config.yml");
        Messages.init(configFile);

        // Initialize CommandAPI (Aikar's ACF)
        commandManager = new CommandRegistry(this);
        commandManager.register(new SolumCommand());

        // Register global listeners
        Bukkit.getPluginManager().registerEvents(new GlobalListener(this), this);

        // Initialize MenuAPI
        menuHandler = new MenuHandler(this);
        Item.setDefaultFormatter(Messages::color);

        // Initialize Database
        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        // Initialize module manager
        moduleManager = new ModuleManager(this);
        registerModules();
        moduleManager.enableAll();

        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SolumExpansion(this).register();
            getLogger().info("[Solum] PlaceholderAPI expansion registered.");
        }

        logStartup();
    }

    @Override
    public void onDisable() {
        if (commandManager != null) {
            commandManager.unregisterAll();
        }
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        INSTANCE = null;
    }

    private void registerModules() {
        List<Function<Solum, ? extends dev.lorenzz.solum.managers.Module>> factories = List.of(
                StatsModule::new, EconomyModule::new, ShardsModule::new, SettingsModule::new,
                WorthModule::new, TeamsModule::new, HomeModule::new, ClearLagModule::new,
                TPAModule::new, SpawnModule::new, AFKModule::new, RTPModule::new,
                ProfileModule::new, KeyAllModule::new, MediaModule::new, InfoModule::new,
                RulesModule::new, AmethystToolsModule::new, LunarModule::new, StatsWipeModule::new,
                BountyModule::new, LeaderboardModule::new, SellModule::new, ShopModule::new,
                BillfordModule::new, HelpModule::new, KitModule::new, AnnouncementModule::new,
                AdModule::new, BroadcastModule::new, EnderChestModule::new, ReportModule::new,
                RequestModule::new, ClearChatModule::new, SkillTreeModule::new, PlaytimeRewardsModule::new,
                BackModule::new, CombatModule::new, WarpModule::new, DuelModule::new,
                ChangelogModule::new, ChatGameModule::new, EasterModule::new,
                ScoreboardModule::new, AuctionHouseModule::new, TablistModule::new,
                JoinQuitModule::new, DoubleJumpModule::new, NametagModule::new
        );
        factories.forEach(f -> moduleManager.registerModule(f.apply(this)));
    }

    public void reloadAllConfigs() {
        configFile.reload();
        Messages.reload(configFile);
        if (moduleManager != null) {
            moduleManager.reloadAll();
        }
        getLogger().info("[Solum] All configurations reloaded.");
    }

    public Config getConfigFile() { return configFile; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public CommandRegistry getCommandManager() { return commandManager; }
    public MenuHandler getMenuHandler() { return menuHandler; }

    private void logStartup() {
        String[] banner = {
                "",
                "&9  ██╗  ██╗██╗██████╗ ██╗   ██╗",
                "&9  ██║  ██║██║██╔══██╗██║   ██║",
                "&9  ███████║██║██████╔╝██║   ██║",
                "&9  ██╔══██║██║██╔══██╗██║   ██║",
                "&9  ██║  ██║██║██║  ██║╚██████╔╝",
                "&9  ╚═╝  ╚═╝╚═╝╚═╝  ╚═╝ ╚═════╝",
                "",
                "&7  Version: &b" + getDescription().getVersion(),
                "&7  Author: &blorenzz",
                "&7  Status: &aEnabled",
                "&7  Modules: &b" + moduleManager.getModules().stream().filter(Module::isEnabled).count() + " active",
                ""
        };
        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(Messages.color(line));
        }
    }
}
