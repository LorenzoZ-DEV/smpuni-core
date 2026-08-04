package dev.lorenzz.lxsmp.modules.statswipe;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.StatsManager;
import dev.lorenzz.lxsmp.utils.GUI;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@CommandAlias("statswipe")
@CommandPermission("solum.admin")
@Description("Wipe all statistics")
public class StatsWipeCommands extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void statswipe(Player player) {
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
        plugin.getLogger().warning("[Solum] Stats wiped by " + player.getName());
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
            plugin.getLogger().severe("[Solum] Failed to write stats wipe log: " + e.getMessage());
        }
    }
}
