package dev.lorenzz.lxsmp.modules.media;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Config;
import dev.lorenzz.lxsmp.utils.GUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

@CommandAlias("media")
@CommandPermission("solum.media")
@Description("Open the media menu")
public class MediaCommands extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void media(Player player) {
        Config config = plugin.getConfigFile();
        String title = config.getString("media.menu-title", "&8&lMedia Menu");
        GUI.Builder builder = GUI.builder(title, 3).border(Material.PURPLE_STAINED_GLASS_PANE);

        ConfigurationSection slots = config.getConfigurationSection("media.slots");
        if (slots != null) {
            for (String key : slots.getKeys(false)) {
                int slot = Integer.parseInt(key);
                Material mat = Material.valueOf(config.getString("media.slots." + key + ".material", "STONE").toUpperCase());
                String name = config.getString("media.slots." + key + ".name", "");
                List<String> lore = config.getStringList("media.slots." + key + ".lore");
                String perm = config.getString("media.slots." + key + ".permission", "");

                if (!perm.isEmpty() && !player.hasPermission(perm)) continue;
                builder.item(slot, mat, name, lore);
            }
        }
        builder.open(player);
    }
}

