package dev.lorenzz.solum.modules.keyall;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import dev.lorenzz.solum.utils.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class KeyAllModule extends AbstractModule {

    private BukkitTask autoKeyAllTask;
    private int seconds;

    public KeyAllModule(Solum plugin) {
        super(plugin, "keyall");
    }

    @Override
    public void onEnable() {
        int interval = plugin.getConfigFile().getInt("KeyAll.every", 3600);
        long lastExecution = plugin.getConfigFile().getLong("KeyAll.last-execution");

        if (lastExecution > 0) {
            long elapsedSeconds = (System.currentTimeMillis() - lastExecution) / 1000L;
            this.seconds = Math.max(1, (int) (interval - elapsedSeconds));
        } else {
            this.seconds = interval;
        }

        autoKeyAllTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            seconds--;

            if (seconds <= 0) {
                seconds = plugin.getConfigFile().getInt("KeyAll.every", 3600);
                List<String> commands = plugin.getConfigFile().getStringList("KeyAll.commands");

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    for (String cmd : commands) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                cmd.replace("{player}", onlinePlayer.getName()));
                    }
                }

                saveTimestamp();
            }
        }, 0L, 20L);
    }

    @Override
    public void onDisable() {
        if (autoKeyAllTask != null) {
            autoKeyAllTask.cancel();
            autoKeyAllTask = null;
        }
        saveTimestamp();
        super.onDisable();
    }

    private void saveTimestamp() {
        Config config = plugin.getConfigFile();
        config.set("KeyAll.last-execution", System.currentTimeMillis());
        config.saveFile();
    }

    public int getSeconds() {
        return seconds;
    }
}