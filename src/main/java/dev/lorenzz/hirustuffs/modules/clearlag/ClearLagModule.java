package dev.lorenzz.hirustuffs.modules.clearlag;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.stream.Collectors;

public class ClearLagModule extends AbstractModule {

    private BukkitTask autoTask;

    public ClearLagModule(Hiru plugin) {
        super(plugin, "clearlag");
    }

    @Override
    public void onEnable() {
        plugin.getCommandManager().register(new ClearLagCommand());

        boolean autoClear = plugin.getConfigFile().getBoolean("clearlag.auto-clear", true);
        if (autoClear) {
            int intervalMinutes = plugin.getConfigFile().getInt("clearlag.interval-minutes", 10);
            int countdownSeconds = plugin.getConfigFile().getInt("clearlag.countdown-seconds", 30);
            autoTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
                    startCountdown(countdownSeconds), intervalMinutes * 60 * 20L, intervalMinutes * 60 * 20L);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (autoTask != null) {
            autoTask.cancel();
            autoTask = null;
        }
    }

    public void startCountdown(int seconds) {
        int[] remaining = {seconds};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (remaining[0] <= 0) {
                task.cancel();
                int cleared = clearEntities();
                String msg = plugin.getConfigFile().getString("clearlag.messages.cleared",
                                "&c&l[ClearLag] &aRimosse &e%amount% &aentità!")
                        .replace("%amount%", String.valueOf(cleared));
                sendToAll(msg);
                return;
            }

            if (remaining[0] == seconds || remaining[0] == 10 || remaining[0] == 5 || remaining[0] <= 3) {
                String msg = plugin.getConfigFile().getString("clearlag.messages.warning",
                                "&c&l[ClearLag] &ePulizia entità tra &c%seconds% &esecondi!")
                        .replace("%seconds%", String.valueOf(remaining[0]));
                sendToAll(msg);
            }

            remaining[0]--;
        }, 0L, 20L);
    }

    public int clearEntities() {
        boolean removeAnimals = plugin.getConfigFile().getBoolean("clearlag.remove-animals", false);
        boolean removeMonsters = plugin.getConfigFile().getBoolean("clearlag.remove-monsters", true);
        boolean removeItems = plugin.getConfigFile().getBoolean("clearlag.remove-items", true);
        boolean removeXP = plugin.getConfigFile().getBoolean("clearlag.remove-experience", false);

        List<String> whitelist = plugin.getConfigFile().getStringList("clearlag.entity-whitelist")
                .stream().map(String::toUpperCase).collect(Collectors.toList());

        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) continue;
                if (whitelist.contains(entity.getType().name())) continue;

                boolean remove = false;
                if (removeItems && entity instanceof Item) remove = true;
                if (removeMonsters && entity instanceof Monster) remove = true;
                if (removeAnimals && entity instanceof Animals) remove = true;
                if (removeXP && entity instanceof ExperienceOrb) remove = true;

                if (remove) {
                    entity.remove();
                    count++;
                }
            }
        }
        return count;
    }

    private void sendToAll(String message) {
        Component component = Messages.colorComponent(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
        Bukkit.getConsoleSender().sendMessage(component);
    }
}
