package dev.lorenzz.lxsmp;

import dev.lorenzz.lxsmp.managers.Module;

import java.util.*;
import java.util.logging.Logger;

public class ModuleManager {

    private static ModuleManager INSTANCE;

    private final LXSMP plugin;
    private final Logger logger;
    private final Map<String, Module> modules = new LinkedHashMap<>();

    public ModuleManager(LXSMP plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        INSTANCE = this;
    }

    public static ModuleManager getInstance() {
        return INSTANCE;
    }

    public void registerModule(Module module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    public void enableAll() {
        for (Module module : modules.values()) {
            try {
                if (module.isEnabled()) {
                    module.onEnable();
                    logger.info("[LXSMP] Module '" + module.getName() + "' enabled.");
                } else {
                    logger.info("[LXSMP] Module '" + module.getName() + "' is disabled in config.");
                }
            } catch (Exception e) {
                logger.severe("[LXSMP] Failed to enable module '" + module.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void disableAll() {
        List<Module> reversed = new ArrayList<>(modules.values());
        Collections.reverse(reversed);
        for (Module module : reversed) {
            try {
                if (module.isEnabled()) {
                    module.onDisable();
                    logger.info("[LXSMP] Module '" + module.getName() + "' disabled.");
                }
            } catch (Exception e) {
                logger.severe("[LXSMP] Failed to disable module '" + module.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void reloadAll() {
        for (Module module : modules.values()) {
            try {
                module.onReload();
                if (module.isEnabled()) {
                    logger.info("[LXSMP] Module '" + module.getName() + "' reloaded.");
                }
            } catch (Exception e) {
                logger.severe("[LXSMP] Failed to reload module '" + module.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(String name) {
        return (T) modules.get(name.toLowerCase());
    }

    public Collection<Module> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }
}
