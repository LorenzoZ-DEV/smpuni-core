package dev.lorenzz.lxsmp.modules.tools;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("giveamethyst")
@CommandPermission("solum.giveamethyst")
@Description("Give an amethyst tool to a player")
public class GiveAmethystCommand extends BaseCommand {

    @Default
    public void give(Player sender, Player target, String toolId) {
        LXSMP plugin = LXSMP.getInstance();
        AmethystToolsModule module = plugin.getModuleManager().getModule("tools");
        if (module == null) {
            Messages.send(sender, "&cAmethystTools module not available.");
            return;
        }

        AmethystToolsListener listener = module.getListener();
        if (listener == null) {
            Messages.send(sender, "&cAmethystTools listener not available.");
            return;
        }

        ItemStack item = listener.createAmethystTool(toolId);
        if (item == null) {
            Messages.send(sender, "&cInvalid tool ID: &e" + toolId + "&c. Check config.yml (tools.tools-list)");
            return;
        }

        target.getInventory().addItem(item);
        Messages.send(sender, "&7Given &d" + toolId + " &7to &b" + target.getName() + "&7.");
        Messages.send(target, "&7You received an &damethyst tool&7!");
    }
}
