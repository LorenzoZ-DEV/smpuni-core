package dev.lorenzz.hirustuffs.modules.statswipe;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.StatsManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StatsWipeCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "statswipe", description = "Wipe all statistics")
    @Permission("hiru.admin")
    public void statswipe(@Sender Player player) {
        String title = plugin.getConfigFile().getString("statswipe.confirm-title", "&c&lAre you sure?");

        GUI.builder(title, 3)
                .border(Material.RED_STAINED_GLASS_PANE)
                .item(11, Material.LIME_CONCRETE, "&a&lConfirm",
                        List.of("&7Click to confirm the stats wipe"),
                        p -> {
                            performWipe(p);
                            p.closeInventory();
                        })
                .item(15, Material.RED_CONCRETE, "&c&lCancel",
                        List.of("&7Click to cancel"),
                        p -> {
                            p.closeInventory();
                            Messages.send(p, plugin.getConfigFile().getString("statswipe.messages.cancelled", "&7Operation cancelled."));
                        })
                .open(player);
    }

    private void performWipe(Player player) {
        StatsManager stats = StatsManager.getInstance();
        if (stats == null) {
            Messages.send(player, "&cStats manager not available.");
            return;
        }
        logWipe(player);
        stats.wipeAll();
        Messages.send(player, plugin.getConfigFile().getString("statswipe.messages.wiped", "&c&lAll stats have been wiped!"));
        plugin.getLogger().warning("[Hiru] Stats wiped by " + player.getName());
    }

    private void logWipe(Player player) {
        String logFileName = plugin.getConfigFile().getString("statswipe.log-file", "statswipe-log.txt");
        File logsDir = new File(plugin.getDataFolder(), "logs");
        logsDir.mkdirs();
        File logFile = new File(logsDir, logFileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.println("[" + timestamp + "] Stats wiped by " + player.getName() + " (" + player.getUniqueId() + ")");
        } catch (IOException e) {
            plugin.getLogger().severe("[Hiru] Failed to write stats wipe log: " + e.getMessage());
        }
    }
}
