package dev.lorenzz.solum.modules.billford;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.managers.BillfordManager;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class BillfordModule extends AbstractModule {

    private BillfordManager billfordManager;
    private BukkitTask rotationTask;

    public BillfordModule(Solum plugin) {
        super(plugin, "billford");
    }

    @Override
    public void onEnable() {
        billfordManager = new BillfordManager(plugin);
        billfordManager.init();
        plugin.getCommandManager().register(new BillfordCommands());

        // Schedule rotation check every 5 minutes
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long next = billfordManager.getNextRotation();
            if (System.currentTimeMillis() >= next) {
                billfordManager.rotateTrades();
                Bukkit.broadcast(Messages.colorComponent(
                        "&#FFD700&l⚡ BILLFORD &#8C8C8C| &#E0E0E0New trades are available! Use &#FFD700/billford &#E0E0E0to check them out!"));
            }
        }, 6000L, 6000L); // Every 5 minutes

        logger.info("[Solum] BillfordModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (rotationTask != null) { rotationTask.cancel(); rotationTask = null; }
    }
}
