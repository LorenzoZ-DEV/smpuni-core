package dev.lorenzz.lxsmp.modules.rules;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.GUI;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("rules")
@Description("View server rules")
public class RulesCommands extends BaseCommand {

    private final RulesModule module;
    private final LXSMP plugin;

    public RulesCommands(RulesModule module) {
        this.module = module;
        this.plugin = LXSMP.getInstance();
    }

    @Default
    public void rules(Player player) {
        openRulesMenu(player);
    }

    @Subcommand("accept")
    @Description("Accept the server rules")
    public void accept(Player player) {
        if (module.hasAccepted(player.getUniqueId())) {
            Messages.send(player, "&#d1e0e0You have already accepted the rules.");
            return;
        }
        module.accept(player.getUniqueId());
        String acceptMsg = plugin.getConfigFile().getString("rules.accept-message", "&#d1e0e0You have accepted the rules!");
        Messages.send(player, acceptMsg);
        player.closeInventory();
    }

    public void openRulesMenu(Player player) {
        String title = plugin.getConfigFile().getString("rules.menu-title", "&#d1e0e0&lServer Rules");
        List<String> rulesList = plugin.getConfigFile().getStringList("rules.rules-list");

        int rows = 6;
        GUI.Builder builder = GUI.builder(title, rows).border(Material.BLACK_STAINED_GLASS_PANE);

        // Display rules in a book-like layout
        builder.item(13, Material.WRITTEN_BOOK, title, rulesList);

        boolean accepted = module.hasAccepted(player.getUniqueId());
        if (!accepted) {
            builder.item(39, Material.LIME_WOOL, "&aAccept Rules",
                    List.of("&7Click to accept the server rules"),
                    p -> {
                        module.accept(p.getUniqueId());
                        String acceptMsg = plugin.getConfigFile().getString("rules.accept-message", "&#d1e0e0You have accepted the rules!");
                        Messages.send(p, acceptMsg);
                        p.closeInventory();
                    });
            builder.item(41, Material.RED_WOOL, "&cClose",
                    List.of("&7Close this menu"),
                    p -> p.closeInventory());
        } else {
            builder.item(40, Material.LIME_WOOL, "&aRules Accepted",
                    List.of("&7You have already accepted the rules."),
                    p -> p.closeInventory());
        }

        builder.open(player);
    }
}

