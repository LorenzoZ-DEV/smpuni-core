package dev.lorenzz.lxsmp.modules.keyall;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
import dev.lorenzz.lxsmp.utils.Config;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class KeyAllModule extends AbstractModule {

    private BukkitTask autoKeyAllTask;
    private long nextExecutionAt;

    public KeyAllModule(LXSMP plugin) {
        super(plugin, "keyall");
    }

    @Override
    public void onEnable() {
        Config config = plugin.getConfigFile();

        boolean autoEnabled = config.getBoolean("keyall.auto-enabled", true);

        int intervalMinutes = config.getInt("keyall.auto-interval-minutes", 60);

        KeyAllCommands.setIntervalMinutes(intervalMinutes);

        if (!autoEnabled || intervalMinutes <= 0) {

            logger.info("[Solum] KeyAllModule loaded (auto-keyall disabled).");

            return;
        }

        long intervalMs = intervalMinutes * 60_000L;
        long lastExecution = config.getLong("keyall.last-execution", 0L);
        long now = System.currentTimeMillis();

        if (lastExecution > 0) {

            KeyAllCommands.setLastKeyAllTime(lastExecution);
            KeyAllCommands.updateLastKeyAll(
                    config.getString("keyall.auto-type", "basic_key"),
                    config.getInt("keyall.auto-amount", 1));

            KeyAllCommands.setLastKeyAllTime(lastExecution);

            nextExecutionAt = lastExecution + intervalMs;
            if (nextExecutionAt <= now) nextExecutionAt = now + 1000L;
        } else {

            KeyAllCommands.setLastKeyAllTime(now);

            config.set("keyall.last-execution", now);
            config.saveFile();
            nextExecutionAt = now + intervalMs;
        }

        autoKeyAllTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (System.currentTimeMillis() < nextExecutionAt) return;

            runAutoKeyAll();
            nextExecutionAt = System.currentTimeMillis() + intervalMs;
            saveTimestamp();
        }, 20L, 20L);

        logger.info("[Solum] KeyAllModule loaded (interval: " + intervalMinutes + "m).");
    }

    @Override
    public void onDisable() {
        if (autoKeyAllTask != null) {
            autoKeyAllTask.cancel();
            autoKeyAllTask = null;
        }
        super.onDisable();
    }

    private void runAutoKeyAll() {
        Config config = plugin.getConfigFile();

        String type = config.getString("keyall.auto-type", "basic_key");
        int amount = config.getInt("keyall.auto-amount", 1);
        String commandTemplate = config.getString("keyall.console-command",
                "crates key give %player% %type% %amount%");

        for (Player player : Bukkit.getOnlinePlayers()) {
            String cmd = commandTemplate
                    .replace("%player%", player.getName())
                    .replace("%type%", type)
                    .replace("%amount%", String.valueOf(amount));

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

            String received = config.getString("keyall.received",
                    "&7You received &b%amount%x %type% Key!")
                    .replace("%amount%", String.valueOf(amount))
                    .replace("%type%", type);

            Messages.send(player, received);
        }

        String broadcast = config.getString("keyall.broadcast",
                "&a&l[KeyAll] &eEveryone received &b%amount%x %type% Key&e!")
                .replace("%amount%", String.valueOf(amount))
                .replace("%type%", type);
        Bukkit.broadcast(Messages.colorComponent(broadcast));

        KeyAllCommands.updateLastKeyAll(type, amount);
    }

    private void saveTimestamp() {
        Config config = plugin.getConfigFile();
        config.set("keyall.last-execution", System.currentTimeMillis());
        config.saveFile();
    }
}