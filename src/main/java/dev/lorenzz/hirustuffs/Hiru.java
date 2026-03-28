package dev.lorenzz.hirustuffs;

import dev.lorenzz.bukkit.BukkitCommandManager;
import dev.lorenzz.hirustuffs.commands.HiruCommand;
import dev.lorenzz.hirustuffs.listeners.GlobalListener;
import dev.lorenzz.hirustuffs.managers.DatabaseManager;
import dev.lorenzz.hirustuffs.managers.Module;
import dev.lorenzz.hirustuffs.modules.afk.AFKModule;
import dev.lorenzz.hirustuffs.modules.billford.BillfordModule;
import dev.lorenzz.hirustuffs.modules.bounty.BountyModule;
import dev.lorenzz.hirustuffs.modules.clearlag.ClearLagModule;
import dev.lorenzz.hirustuffs.modules.economy.EconomyModule;
import dev.lorenzz.hirustuffs.modules.enderchest.EnderChestModule;
import dev.lorenzz.hirustuffs.modules.help.HelpModule;
import dev.lorenzz.hirustuffs.modules.homes.HomeModule;
import dev.lorenzz.hirustuffs.modules.info.InfoModule;
import dev.lorenzz.hirustuffs.modules.keyall.KeyAllModule;
import dev.lorenzz.hirustuffs.modules.leaderboard.LeaderboardModule;
import dev.lorenzz.hirustuffs.modules.lunar.LunarModule;
import dev.lorenzz.hirustuffs.modules.media.MediaModule;
import dev.lorenzz.hirustuffs.modules.profile.ProfileModule;
import dev.lorenzz.hirustuffs.modules.rtp.RTPModule;
import dev.lorenzz.hirustuffs.modules.rules.RulesModule;
import dev.lorenzz.hirustuffs.modules.sell.SellModule;
import dev.lorenzz.hirustuffs.modules.settings.SettingsModule;
import dev.lorenzz.hirustuffs.modules.shards.ShardsModule;
import dev.lorenzz.hirustuffs.modules.shop.ShopModule;
import dev.lorenzz.hirustuffs.modules.spawn.SpawnModule;
import dev.lorenzz.hirustuffs.modules.stats.StatsModule;
import dev.lorenzz.hirustuffs.modules.statswipe.StatsWipeModule;
import dev.lorenzz.hirustuffs.modules.teams.TeamsModule;
import dev.lorenzz.hirustuffs.modules.tools.AmethystToolsModule;
import dev.lorenzz.hirustuffs.modules.tpa.TPAModule;
import dev.lorenzz.hirustuffs.modules.ad.AdModule;
import dev.lorenzz.hirustuffs.modules.announcements.AnnouncementModule;
import dev.lorenzz.hirustuffs.modules.broadcast.BroadcastModule;
import dev.lorenzz.hirustuffs.modules.kits.KitModule;
import dev.lorenzz.hirustuffs.modules.report.ReportModule;
import dev.lorenzz.hirustuffs.modules.request.RequestModule;
import dev.lorenzz.hirustuffs.modules.clearchat.ClearChatModule;
import dev.lorenzz.hirustuffs.modules.skilltree.SkillTreeModule;
import dev.lorenzz.hirustuffs.modules.playtimerewards.PlaytimeRewardsModule;
import dev.lorenzz.hirustuffs.modules.worth.WorthModule;
import dev.lorenzz.hirustuffs.modules.back.BackModule;
import dev.lorenzz.hirustuffs.modules.combat.CombatModule;
import dev.lorenzz.hirustuffs.placeholders.HiruExpansion;
import dev.lorenzz.hirustuffs.utils.Config;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.j4c0b3y.api.menu.MenuHandler;
import net.j4c0b3y.api.menu.utils.Item;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Hiru extends JavaPlugin {

    private static Hiru INSTANCE;

    private Config configFile;
    private ModuleManager moduleManager;
    private BukkitCommandManager commandManager;
    private DatabaseManager databaseManager;
    private MenuHandler menuHandler;

    public static Hiru getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        // Load config
        configFile = new Config(this, "config.yml");
        Messages.init(configFile);

        // Initialize CommandAPI
        commandManager = new BukkitCommandManager(this);
        commandManager.register(new HiruCommand());

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
            new HiruExpansion(this).register();
            getLogger().info("[Hiru] PlaceholderAPI expansion registered.");
        }

        logStartup();
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        INSTANCE = null;
    }

    private void registerModules() {
        moduleManager.registerModule(new StatsModule(this));
        moduleManager.registerModule(new EconomyModule(this));
        moduleManager.registerModule(new ShardsModule(this));
        moduleManager.registerModule(new SettingsModule(this));
        moduleManager.registerModule(new WorthModule(this));
        moduleManager.registerModule(new TeamsModule(this));
        moduleManager.registerModule(new HomeModule(this));
        moduleManager.registerModule(new ClearLagModule(this));
        moduleManager.registerModule(new TPAModule(this));
        moduleManager.registerModule(new SpawnModule(this));
        moduleManager.registerModule(new AFKModule(this));
        moduleManager.registerModule(new RTPModule(this));
        moduleManager.registerModule(new ProfileModule(this));
        moduleManager.registerModule(new KeyAllModule(this));
        moduleManager.registerModule(new MediaModule(this));
        moduleManager.registerModule(new InfoModule(this));
        moduleManager.registerModule(new RulesModule(this));
        moduleManager.registerModule(new AmethystToolsModule(this));
        moduleManager.registerModule(new LunarModule(this));
        moduleManager.registerModule(new StatsWipeModule(this));
        moduleManager.registerModule(new BountyModule(this));
        moduleManager.registerModule(new LeaderboardModule(this));
        moduleManager.registerModule(new SellModule(this));
        moduleManager.registerModule(new ShopModule(this));
        moduleManager.registerModule(new BillfordModule(this));
        moduleManager.registerModule(new HelpModule(this));
        moduleManager.registerModule(new KitModule(this));
        moduleManager.registerModule(new AnnouncementModule(this));
        moduleManager.registerModule(new AdModule(this));
        moduleManager.registerModule(new BroadcastModule(this));
        moduleManager.registerModule(new EnderChestModule(this));
        moduleManager.registerModule(new ReportModule(this));
        moduleManager.registerModule(new RequestModule(this));
        moduleManager.registerModule(new ClearChatModule(this));
        moduleManager.registerModule(new SkillTreeModule(this));
        moduleManager.registerModule(new PlaytimeRewardsModule(this));
        moduleManager.registerModule(new BackModule(this));
        moduleManager.registerModule(new CombatModule(this));
    }

    public void reloadAllConfigs() {
        configFile.reload();
        Messages.reload(configFile);
        if (moduleManager != null) {
            moduleManager.reloadAll();
        }
        getLogger().info("[Hiru] All configurations reloaded.");
    }

    public Config getConfigFile() { return configFile; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public BukkitCommandManager getCommandManager() { return commandManager; }
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
