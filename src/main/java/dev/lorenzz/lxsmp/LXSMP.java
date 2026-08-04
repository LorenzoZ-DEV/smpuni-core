package dev.lorenzz.lxsmp;

import dev.lorenzz.lxsmp.commands.CommandRegistry;
import dev.lorenzz.lxsmp.commands.SolumCommand;
import dev.lorenzz.lxsmp.listeners.GlobalListener;
import dev.lorenzz.lxsmp.managers.DatabaseManager;
import dev.lorenzz.lxsmp.managers.Module;
import dev.lorenzz.lxsmp.modules.adaptive.AdaptiveToolModule;
import dev.lorenzz.lxsmp.modules.afk.AFKModule;
import dev.lorenzz.lxsmp.modules.billford.BillfordModule;
import dev.lorenzz.lxsmp.modules.bounty.BountyModule;
import dev.lorenzz.lxsmp.modules.chat.ChatFormatModule;
import dev.lorenzz.lxsmp.modules.clearlag.ClearLagModule;
import dev.lorenzz.lxsmp.modules.economy.EconomyModule;
import dev.lorenzz.lxsmp.modules.enderchest.EnderChestModule;
import dev.lorenzz.lxsmp.modules.help.HelpModule;
import dev.lorenzz.lxsmp.modules.homes.HomeModule;
import dev.lorenzz.lxsmp.modules.info.InfoModule;
import dev.lorenzz.lxsmp.modules.joinquit.JoinQuitModule;
import dev.lorenzz.lxsmp.modules.keyall.KeyAllModule;
import dev.lorenzz.lxsmp.modules.leaderboard.LeaderboardModule;
import dev.lorenzz.lxsmp.modules.lunar.LunarModule;
import dev.lorenzz.lxsmp.modules.media.MediaModule;
import dev.lorenzz.lxsmp.modules.profile.ProfileModule;
import dev.lorenzz.lxsmp.modules.rtp.RTPModule;
import dev.lorenzz.lxsmp.modules.rules.RulesModule;
import dev.lorenzz.lxsmp.modules.sell.SellModule;
import dev.lorenzz.lxsmp.modules.scoreboard.ScoreboardModule;
import dev.lorenzz.lxsmp.modules.auctionhouse.AuctionHouseModule;
import dev.lorenzz.lxsmp.modules.tablist.TablistModule;
import dev.lorenzz.lxsmp.modules.settings.SettingsModule;
import dev.lorenzz.lxsmp.modules.shards.ShardsModule;
import dev.lorenzz.lxsmp.modules.shop.ShopModule;
import dev.lorenzz.lxsmp.modules.spawn.SpawnModule;
import dev.lorenzz.lxsmp.modules.stats.StatsModule;
import dev.lorenzz.lxsmp.modules.statswipe.StatsWipeModule;
import dev.lorenzz.lxsmp.modules.teams.TeamsModule;
import dev.lorenzz.lxsmp.modules.tools.AmethystToolsModule;
import dev.lorenzz.lxsmp.modules.tpa.TPAModule;
import dev.lorenzz.lxsmp.modules.announcements.AnnouncementModule;
import dev.lorenzz.lxsmp.modules.broadcast.BroadcastModule;
import dev.lorenzz.lxsmp.modules.kits.KitModule;
import dev.lorenzz.lxsmp.modules.report.ReportModule;
import dev.lorenzz.lxsmp.modules.request.RequestModule;
import dev.lorenzz.lxsmp.modules.clearchat.ClearChatModule;
import dev.lorenzz.lxsmp.modules.skilltree.SkillTreeModule;
import dev.lorenzz.lxsmp.modules.playtimerewards.PlaytimeRewardsModule;
import dev.lorenzz.lxsmp.modules.worth.WorthModule;
import dev.lorenzz.lxsmp.modules.back.BackModule;
import dev.lorenzz.lxsmp.modules.combat.CombatModule;
import dev.lorenzz.lxsmp.modules.warp.WarpModule;
import dev.lorenzz.lxsmp.modules.duel.DuelModule;
import dev.lorenzz.lxsmp.modules.changelog.ChangelogModule;
import dev.lorenzz.lxsmp.modules.chatgames.ChatGameModule;
import dev.lorenzz.lxsmp.modules.doublejump.DoubleJumpModule;
import dev.lorenzz.lxsmp.modules.easter.EasterModule;
import dev.lorenzz.lxsmp.modules.nametag.NametagModule;
import dev.lorenzz.lxsmp.placeholders.SolumExpansion;
import dev.lorenzz.lxsmp.utils.Config;
import dev.lorenzz.lxsmp.utils.Messages;
import net.j4c0b3y.api.menu.MenuHandler;
import net.j4c0b3y.api.menu.utils.Item;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Function;

public final class LXSMP extends JavaPlugin {

    private static LXSMP INSTANCE;

    private Config configFile;
    private ModuleManager moduleManager;
    private CommandRegistry commandManager;
    private DatabaseManager databaseManager;
    private MenuHandler menuHandler;

    public static LXSMP getInstance() {
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
            getLogger().info("[LXSMP] PlaceholderAPI expansion registered.");
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
        List<Function<LXSMP, ? extends dev.lorenzz.lxsmp.managers.Module>> factories = List.of(
                StatsModule::new, EconomyModule::new, ShardsModule::new, SettingsModule::new,
                WorthModule::new, TeamsModule::new, HomeModule::new, ClearLagModule::new,
                TPAModule::new, SpawnModule::new, AFKModule::new, RTPModule::new,
                ProfileModule::new, KeyAllModule::new, MediaModule::new, InfoModule::new,
                RulesModule::new, AmethystToolsModule::new, LunarModule::new, StatsWipeModule::new,
                BountyModule::new, LeaderboardModule::new, SellModule::new, ShopModule::new,
                BillfordModule::new, HelpModule::new, KitModule::new, AnnouncementModule::new,
                BroadcastModule::new, EnderChestModule::new, ReportModule::new,
                RequestModule::new, ClearChatModule::new, SkillTreeModule::new, PlaytimeRewardsModule::new,
                BackModule::new, CombatModule::new, WarpModule::new, DuelModule::new,
                ChangelogModule::new, ChatGameModule::new, EasterModule::new,
                ScoreboardModule::new, AuctionHouseModule::new, TablistModule::new,
                JoinQuitModule::new, DoubleJumpModule::new, NametagModule::new,
                AdaptiveToolModule::new,
                ChatFormatModule::new
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
