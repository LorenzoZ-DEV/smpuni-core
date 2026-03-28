package dev.lorenzz.hirustuffs.modules.media;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Config;
import dev.lorenzz.hirustuffs.utils.GUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public class MediaCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "media", description = "Open the media menu")
    @Permission("hiru.media")
    public void media(@Sender Player player) {
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
