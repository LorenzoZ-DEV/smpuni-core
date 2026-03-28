package dev.lorenzz.hirustuffs.modules.tools;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveAmethystCommand {

    @Command(name = "giveamethyst", description = "Give an amethyst tool to a player")
    @Permission("hiru.giveamethyst")
    public void give(@Sender Player sender, @Named("player") Player target, @Named("tool") String toolId) {
        Hiru plugin = Hiru.getInstance();
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
