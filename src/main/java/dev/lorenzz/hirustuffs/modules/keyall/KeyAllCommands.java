package dev.lorenzz.hirustuffs.modules.keyall;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class KeyAllCommands {

    private final Hiru plugin = Hiru.getInstance();

    // Static fields accessible from placeholder
    private static long lastKeyAllTime = 0;
    private static String lastKeyAllType = "";
    private static int lastKeyAllAmount = 0;
    private static int intervalMinutes = 60;

    public static long getLastKeyAllTime() { return lastKeyAllTime; }
    public static String getLastKeyAllType() { return lastKeyAllType; }
    public static int getLastKeyAllAmount() { return lastKeyAllAmount; }

    public static void setIntervalMinutes(int minutes) { intervalMinutes = minutes; }

    public static void updateLastKeyAll(String type, int amount) {
        lastKeyAllTime = System.currentTimeMillis();
        lastKeyAllType = type;
        lastKeyAllAmount = amount;
    }

    /**
     * Returns a formatted string showing the time remaining until the next auto keyall.
     * If no keyall has happened yet, shows the full interval.
     */
    public static String getTimeUntilNextKeyAll() {
        if (intervalMinutes <= 0) return "N/A";
        long intervalMs = intervalMinutes * 60L * 1000L;
        long elapsed;
        if (lastKeyAllTime == 0) {
            // No keyall yet — assume started at server start, show full interval
            elapsed = 0;
        } else {
            elapsed = System.currentTimeMillis() - lastKeyAllTime;
        }
        long remaining = intervalMs - elapsed;
        if (remaining <= 0) return "Soon!";

        long totalSeconds = remaining / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    @Command(name = "keyall", description = "Give keys to all online players")
    @Permission("hiru.admin")
    public void keyall(@Sender CommandSender sender, @Named("type") String keyType, @Range(min = 1) @Named("amount") int amount) {
        List<String> validTypes = plugin.getConfigFile().getStringList("keyall.valid-types");
        if (!validTypes.contains(keyType.toLowerCase())) {
            Messages.send(sender, "&cInvalid type! Types: " + String.join(", ", validTypes));
            return;
        }

        // Get the console command template from config
        String commandTemplate = plugin.getConfigFile().getString("keyall.console-command",
                "crate give %player% %type% %amount%");

        for (Player player : Bukkit.getOnlinePlayers()) {
            String cmd = commandTemplate
                    .replace("%player%", player.getName())
                    .replace("%type%", keyType.toLowerCase())
                    .replace("%amount%", String.valueOf(amount));

            // Execute command from console
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

            Messages.send(player, plugin.getConfigFile().getString("keyall.received", "&7You received &b%amount%x %type% Key!")
                    .replace("%amount%", String.valueOf(amount)).replace("%type%", keyType));
        }

        // Update last keyall info for placeholder
        updateLastKeyAll(keyType, amount);

        Bukkit.broadcast(Messages.colorComponent(
                plugin.getConfigFile().getString("keyall.broadcast", "&a&l[KeyAll] &eEveryone received &b%amount%x %type% Key&e!")
                        .replace("%amount%", String.valueOf(amount)).replace("%type%", keyType)));
    }
}
