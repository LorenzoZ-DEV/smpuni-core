package dev.lorenzz.solum.modules.adaptive;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("giveadaptive|adaptivetool")
@CommandPermission("solum.giveadaptive")
@Description("Give an Adaptive Tool to a player")
public class GiveAdaptiveToolCommand extends BaseCommand {

    @Default
    public void give(CommandSender sender, @Optional Player target) {
        Solum plugin = Solum.getInstance();
        AdaptiveToolModule module = plugin.getModuleManager().getModule("adaptive-tool");
        if (module == null || module.getListener() == null) {
            Messages.send(sender, "&cAdaptiveTool module not available.");
            return;
        }

        Player receiver = target != null ? target : (sender instanceof Player p ? p : null);
        if (receiver == null) {
            Messages.send(sender, "&cConsole must specify a target player.");
            return;
        }

        ItemStack item = module.getListener().createTool();
        if (item == null) {
            Messages.send(sender, "&cFailed to build adaptive tool. Check config.yml (adaptive-tool.forms).");
            return;
        }

        receiver.getInventory().addItem(item);
        Messages.send(sender, "&7Given &5Adaptive Tool &7to &b" + receiver.getName() + "&7.");
        if (!receiver.equals(sender)) {
            Messages.send(receiver, "&7You received an &5Adaptive Tool&7!");
        }
    }
}

