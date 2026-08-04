package dev.lorenzz.lxsmp.managers;

import dev.lorenzz.lxsmp.LXSMP;

public interface Module {

    /**
     * Unique identifier for this module (matches config key under 'modules')
     */
    String getName();

    /**
     * Whether this module is enabled in config
     */
    default boolean isEnabled() {
        return LXSMP.getInstance().getConfigFile().getBoolean("modules." + getName(), true);
    }

    /**
     * Called when the module is enabled
     */
    void onEnable();

    /**
     * Called when the module is disabled
     */
    void onDisable();

    /**
     * Called when config is reloaded
     */
    default void onReload() {
        onDisable();
        if (isEnabled()) {
            onEnable();
        }
    }
}
