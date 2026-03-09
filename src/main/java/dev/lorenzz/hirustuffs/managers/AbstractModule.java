package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.logging.Logger;

public abstract class AbstractModule implements Module {

    protected final Hiru plugin;
    protected final Logger logger;
    private final String name;
    private final List<Listener> registeredListeners = new ArrayList<>();

    protected AbstractModule(Hiru plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.logger = plugin.getLogger();
    }

    @Override
    public String getName() {
        return name;
    }

    protected void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        registeredListeners.add(listener);
    }

    protected void unregisterListeners() {
        for (Listener listener : registeredListeners) {
            HandlerList.unregisterAll(listener);
        }
        registeredListeners.clear();
    }

    @Override
    public void onDisable() {
        unregisterListeners();
    }

    @Override
    public void onReload() {
        onDisable();
        if (isEnabled()) {
            onEnable();
        }
    }
}
