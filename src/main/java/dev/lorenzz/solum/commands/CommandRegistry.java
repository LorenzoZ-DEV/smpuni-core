package dev.lorenzz.solum.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import dev.lorenzz.solum.Solum;

/**
 * Thin wrapper around Aikar's {@link PaperCommandManager} so module classes can keep
 * calling {@code plugin.getCommandManager().register(new XCommand())} unchanged after
 * the migration from the legacy custom command API.
 */
public class CommandRegistry {

    private final PaperCommandManager manager;

    public CommandRegistry(Solum plugin) {
        this.manager = new PaperCommandManager(plugin);
        // Allow brigadier-based completions on Paper 1.20+.
        try {
            this.manager.enableUnstableAPI("help");
            this.manager.enableUnstableAPI("brigadier");
        } catch (Throwable ignored) {
            // older paper builds may not expose brigadier; safe to ignore.
        }
    }

    /**
     * Registers an ACF command. Kept named {@code register} (instead of ACF's
     * {@code registerCommand}) to preserve compatibility with the previous custom API
     * usage already spread across all module classes.
     */
    public void register(BaseCommand command) {
        manager.registerCommand(command);
    }

    public PaperCommandManager getManager() {
        return manager;
    }

    public void unregisterAll() {
        manager.unregisterCommands();
    }
}

