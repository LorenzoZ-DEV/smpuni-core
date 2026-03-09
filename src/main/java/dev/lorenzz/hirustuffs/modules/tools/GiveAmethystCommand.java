package dev.lorenzz.hirustuffs.modules.tools;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveAmethystCommand {

    @Command(name = "giveamethyst", description = "Dai uno strumento in ametista a un giocatore")
    @Permission("hiru.giveamethyst")
    public void give(@Sender Player sender, @Named("giocatore") Player target, @Named("tool") String toolId) {
        Hiru plugin = Hiru.getInstance();
        AmethystToolsModule module = plugin.getModuleManager().getModule("tools");
        if (module == null) {
            Messages.send(sender, "&cModulo AmethystTools non disponibile.");
            return;
        }

        AmethystToolsListener listener = module.getListener();
        if (listener == null) {
            Messages.send(sender, "&cAmethystTools listener non disponibile.");
            return;
        }

        ItemStack item = listener.createAmethystTool(toolId);
        if (item == null) {
            Messages.send(sender, "&cTool ID non valido: &e" + toolId + "&c. Controlla config.yml (tools.tools-list)");
            return;
        }

        target.getInventory().addItem(item);
        Messages.send(sender, "&7Dato &d" + toolId + " &7a &b" + target.getName() + "&7.");
        Messages.send(target, "&7Hai ricevuto uno strumento in &dametista&7!");
    }
}
