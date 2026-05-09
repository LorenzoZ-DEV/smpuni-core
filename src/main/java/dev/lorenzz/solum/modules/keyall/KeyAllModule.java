package dev.lorenzz.solum.modules.keyall;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class KeyAllModule extends AbstractModule {

    private BukkitTask autoKeyAllTask;

    public KeyAllModule(Solum plugin) {
        super(plugin, "keyall");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new KeyAllCommands());

        // Auto KeyAll timer
        int intervalMinutes = plugin.getConfigFile().getInt("keyall.auto-interval-minutes", 60);
        boolean autoEnabled = plugin.getConfigFile().getBoolean("keyall.auto-enabled", false);
        String autoType = plugin.getConfigFile().getString("keyall.auto-type", "common");
        int autoAmount = plugin.getConfigFile().getInt("keyall.auto-amount", 1);

        // Set the interval for the countdown placeholder
        KeyAllCommands.setIntervalMinutes(intervalMinutes);

        if (autoEnabled && intervalMinutes > 0) {
            long intervalTicks = intervalMinutes * 60L * 20L;
            autoKeyAllTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                String commandTemplate = plugin.getConfigFile().getString("keyall.console-command",
                        "crates give %player% %type% %amount%");

                for (Player player : Bukkit.getOnlinePlayers()) {
                    String cmd = commandTemplate
                            .replace("%player%", player.getName())
                            .replace("%type%", autoType)
                            .replace("%amount%", String.valueOf(autoAmount));
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

                    Messages.send(player, plugin.getConfigFile().getString("keyall.received",
                                    "&7You received &b%amount%x %type% Key!")
                            .replace("%amount%", String.valueOf(autoAmount)).replace("%type%", autoType));
                }

                KeyAllCommands.updateLastKeyAll(autoType, autoAmount);

                Bukkit.broadcast(Messages.colorComponent(
                        plugin.getConfigFile().getString("keyall.broadcast",
                                        "&a&l[KeyAll] &eEveryone received &b%amount%x %type% Key&e!")
                                .replace("%amount%", String.valueOf(autoAmount)).replace("%type%", autoType)));
            }, intervalTicks, intervalTicks);
        }

        logger.info("[Solum] KeyAllModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (autoKeyAllTask != null) {
            autoKeyAllTask.cancel();
            autoKeyAllTask = null;
        }
    }
}
