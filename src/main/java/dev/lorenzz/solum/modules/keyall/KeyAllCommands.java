package dev.lorenzz.solum.modules.keyall;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("keyall")
@CommandPermission("solum.admin")
@Description("Give keys to all online players")
public class KeyAllCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();

    private static long lastKeyAllTime = 0;
    private static String lastKeyAllType = "";
    private static int lastKeyAllAmount = 0;
    private static int intervalMinutes = 60;

    public static long getLastKeyAllTime() { return lastKeyAllTime; }
    public static String getLastKeyAllType() { return lastKeyAllType; }
    public static int getLastKeyAllAmount() { return lastKeyAllAmount; }

    public static void setIntervalMinutes(int minutes) { intervalMinutes = minutes; }

    public static void setLastKeyAllTime(long timestampMs) {
        lastKeyAllTime = timestampMs;
    }

    public static void updateLastKeyAll(String type, int amount) {
        lastKeyAllTime = System.currentTimeMillis();
        lastKeyAllType = type;
        lastKeyAllAmount = amount;
    }

    public static long getRemainingSeconds() {
        if (intervalMinutes <= 0) return 0;
        if (lastKeyAllTime == 0) return 0;
        long intervalMs = intervalMinutes * 60L * 1000L;
        long remaining = (lastKeyAllTime + intervalMs) - System.currentTimeMillis();
        return remaining <= 0 ? 0 : remaining / 1000L;
    }

    public static String getTimeUntilNextKeyAll() {
        if (intervalMinutes <= 0) return "N/A";
        if (lastKeyAllTime == 0) return "N/A";
        long intervalMs = intervalMinutes * 60L * 1000L;
        long remaining = (lastKeyAllTime + intervalMs) - System.currentTimeMillis();
        if (remaining <= 0) return "Soon!";

        long totalSeconds = remaining / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    @Default
    public void keyall(CommandSender sender, String keyType, int amount) {
        if (amount < 1) { Messages.send(sender, "&cAmount must be at least 1!"); return; }

        List<String> validTypes = plugin.getConfigFile().getStringList("keyall.valid-types");
        if (!validTypes.contains(keyType.toLowerCase())) {
            Messages.send(sender, "&cInvalid type! Types: " + String.join(", ", validTypes));
            return;
        }

        String commandTemplate = plugin.getConfigFile().getString("keyall.console-command",
                "crate give %player% %type% %amount%");

        for (Player player : Bukkit.getOnlinePlayers()) {
            String cmd = commandTemplate
                    .replace("%player%", player.getName())
                    .replace("%type%", keyType.toLowerCase())
                    .replace("%amount%", String.valueOf(amount));

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

            Messages.send(player, plugin.getConfigFile().getString("keyall.received", "&7You received &b%amount%x %type% Key!")
                    .replace("%amount%", String.valueOf(amount)).replace("%type%", keyType));
        }

        updateLastKeyAll(keyType, amount);

        Bukkit.broadcast(Messages.colorComponent(
                plugin.getConfigFile().getString("keyall.broadcast", "&a&l[KeyAll] &eEveryone received &b%amount%x %type% Key&e!")
                        .replace("%amount%", String.valueOf(amount)).replace("%type%", keyType)));
    }
}

