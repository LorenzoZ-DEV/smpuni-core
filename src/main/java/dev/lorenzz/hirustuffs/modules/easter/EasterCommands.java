package dev.lorenzz.hirustuffs.modules.easter;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EasterCommands {

    private final EasterManager manager;

    public EasterCommands(EasterManager manager) {
        this.manager = manager;
    }

    @Command(name = "easter", description = "Easter Egg Hunt commands", async = false)
    public void easter(@Sender CommandSender sender) {
        Messages.sendRaw(sender, "");
        Messages.sendRaw(sender, "&#FFB6C1&l✦ &#FFFACD&lEASTER EGG HUNT &#FFB6C1&l✦");
        Messages.sendRaw(sender, "");

        if (!manager.isActive()) {
            if (manager.isWithinEventDates()) {
                Messages.sendRaw(sender, "&#FFFACDThe event is within the active period but not started yet.");
            } else {
                Messages.sendRaw(sender, "&#FF6B6BThe event is not currently active.");
            }
        } else {
            Messages.sendRaw(sender, "&#A8E6CFThe event is active!");
            Messages.sendRaw(sender, "&#E8E8E8Goal: &#A8E6CF" + manager.getTotalEggsGoal() + " eggs");
            Messages.sendRaw(sender, "&#E8E8E8First to &#A8E6CF" + manager.getFirstPlaceEggs() + " eggs &#E8E8E8wins!");
            Messages.sendRaw(sender, "&#E8E8E8Time remaining: &#FFFACD" + manager.getTimeRemaining());
        }

        Messages.sendRaw(sender, "");
        Messages.sendRaw(sender, "&#FFFACD&l➤ &#F8E1E81st Place: &#FFD700VIP");
        Messages.sendRaw(sender, "&#FFFACD&l➤ &#F8E1E82nd Place: &#98D8C8Crate Key");
        Messages.sendRaw(sender, "&#FFFACD&l➤ &#F8E1E83rd Place: &#D8A7D8VIP Kit");
        Messages.sendRaw(sender, "");
        Messages.sendRaw(sender, "&#E8E8E8Commands: &#FFFACD/easter top &#E8E8E8| &#FFFACD/easter info");
        Messages.sendRaw(sender, "");
    }

    @Subcommand(name = "start", description = "Start the Easter Egg Hunt event")
    @Permission("hiru.admin")
    public void start(@Sender CommandSender sender) {
        if (manager.isActive()) {
            Messages.send(sender, "&#FF6B6BThe Easter Egg Hunt is already active!");
            return;
        }
        manager.start();
        Messages.send(sender, "&#A8E6CFEaster Egg Hunt started!");
    }

    @Subcommand(name = "stop", description = "Stop the Easter Egg Hunt event")
    @Permission("hiru.admin")
    public void stop(@Sender CommandSender sender) {
        if (!manager.isActive()) {
            Messages.send(sender, "&#FF6B6BThe Easter Egg Hunt is not active!");
            return;
        }
        manager.stop();
        Messages.send(sender, "&#FF6B6BEaster Egg Hunt stopped!");
    }

    @Subcommand(name = "end", description = "End the event and give prizes")
    @Permission("hiru.admin")
    public void end(@Sender CommandSender sender) {
        if (!manager.isActive()) {
            Messages.send(sender, "&#FF6B6BThe Easter Egg Hunt is not active!");
            return;
        }
        manager.endWithPrizes();
        Messages.send(sender, "&#A8E6CFEaster Egg Hunt ended, prizes awarded!");
    }

    @Subcommand(name = "top", description = "Show the Easter Egg leaderboard")
    public void top(@Sender CommandSender sender) {
        if (!manager.isActive()) {
            Messages.send(sender, "&#FF6B6BThe Easter Egg Hunt is not active right now!");
            return;
        }

        Messages.sendRaw(sender, "");
        Messages.sendRaw(sender, "&#FFB6C1&l✦ &#FFFACD&lEASTER EGG LEADERBOARD &#FFB6C1&l✦");
        Messages.sendRaw(sender, "");
        Messages.sendRaw(sender, "&#E8E8E8Total eggs found: &#A8E6CF" + manager.getTotalCollected() + "&#E8E8E8/&#A8E6CF" + manager.getTotalEggsGoal());
        Messages.sendRaw(sender, "&#E8E8E8Eggs remaining: &#FFFACD" + manager.getRemainingEggs());
        Messages.sendRaw(sender, "");

        List<Map.Entry<UUID, Integer>> topList = manager.getTopPlayers(10);
        if (topList.isEmpty()) {
            Messages.sendRaw(sender, "&#E8E8E8No one has found any eggs yet!");
        } else {
            String[] medals = {"&#FFD700&l🥇", "&#C0C0C0&l🥈", "&#CD7F32&l🥉"};
            String[] prizes = {"&#FFD700VIP", "&#98D8C8Crate Key", "&#D8A7D8VIP Kit"};
            for (int i = 0; i < topList.size(); i++) {
                Map.Entry<UUID, Integer> entry = topList.get(i);
                String name = manager.getPlayerNames().getOrDefault(entry.getKey(), "???");
                String prefix = i < 3 ? medals[i] : "&#E8E8E8&l#" + (i + 1);
                String prize = i < 3 ? " &#E8E8E8→ " + prizes[i] : "";
                Messages.sendRaw(sender, prefix + " &#FFFACD" + name + " &#E8E8E8- &#A8E6CF" + entry.getValue() + " eggs" + prize);
            }
        }

        // Show personal stats if player
        if (sender instanceof Player player) {
            int myEggs = manager.getPlayerEggs(player.getUniqueId());
            Messages.sendRaw(sender, "");
            Messages.sendRaw(sender, "&#E8E8E8Your eggs: &#FFB6C1" + myEggs);
        }
        Messages.sendRaw(sender, "");
    }

    @Subcommand(name = "info", description = "Easter Egg Hunt event info")
    public void info(@Sender CommandSender sender) {
        // Same as root command
        easter(sender);
    }
}
