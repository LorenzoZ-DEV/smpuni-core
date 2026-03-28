package dev.lorenzz.hirustuffs.modules.rules;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class RulesCommands {

    private final Hiru plugin = Hiru.getInstance();
    private final RulesModule module;

    public RulesCommands(RulesModule module) {
        this.module = module;
    }

    @Command(name = "rules", description = "Server rules")
    @Permission("hiru.rules")
    public void rules(@Sender Player player) {
        openRulesMenu(player);
    }

    public void openRulesMenu(Player player) {
        List<String> rules = plugin.getConfigFile().getStringList("rules.rules-list");
        String title = plugin.getConfigFile().getString("rules.menu-title", "&8&lServer Rules");

        int rows = Math.min(Math.max((rules.size() / 7) + 3, 3), 6);
        GUI.Builder builder = GUI.builder(title, rows).border(Material.RED_STAINED_GLASS_PANE);

        int slot = 10;
        for (String rule : rules) {
            if (slot >= (rows * 9) - 9) break;
            builder.item(slot, Material.PAPER, rule, List.of());
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (!module.hasAccepted(player.getUniqueId())) {
            builder.item((rows * 9) - 5, Material.LIME_CONCRETE, "&a&lAccept Rules",
                    List.of("&7Click to accept the rules"),
                    p -> {
                        module.accept(p.getUniqueId());
                        p.closeInventory();
                        Messages.send(p, plugin.getConfigFile().getString("rules.accept-message", "&aYou have accepted the rules!"));
                    });
            builder.item((rows * 9) - 3, Material.RED_CONCRETE, "&c&lDecline",
                    List.of("&7Click to decline"),
                    p -> {
                        p.closeInventory();
                        // Kick player if they refuse
                        String denyMsg = plugin.getConfigFile().getString("rules.deny-message", "&cYou must accept the rules to play!");
                        p.kick(Messages.colorComponent(denyMsg));
                    });
        } else {
            builder.item((rows * 9) - 4, Material.LIME_CONCRETE, "&a&lRules Already Accepted",
                    List.of("&7You have already accepted the rules!"));
        }

        builder.open(player);
    }
}
