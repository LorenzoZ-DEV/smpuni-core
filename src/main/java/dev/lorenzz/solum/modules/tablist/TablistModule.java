package dev.lorenzz.solum.modules.tablist;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import org.bukkit.Bukkit;

public class TablistModule extends AbstractModule {

    private TablistManager tablistManager;
    private TablistListener listener;

    public TablistModule(Solum plugin) {
        super(plugin, "tablist");
    }

    public TablistManager getTablistManager() {
        return tablistManager;
    }

    @Override
    public void onEnable() {
        tablistManager = new TablistManager(plugin);
        tablistManager.start();
        listener = new TablistListener(tablistManager);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        logger.info("[Solum] TablistModule loaded (vanilla-style, columns scale with player count).");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (tablistManager != null) {
            tablistManager.stop();
            tablistManager = null;
        }
        listener = null;
    }

    @Override
    public void onReload() {
        if (tablistManager != null) {
            tablistManager.reloadConfig();
            tablistManager.stop();
            tablistManager.start();
        }
    }
}

