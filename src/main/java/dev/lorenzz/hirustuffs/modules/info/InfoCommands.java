package dev.lorenzz.hirustuffs.modules.info;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Config;
import dev.lorenzz.hirustuffs.utils.GUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class InfoCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "serverinfo", aliases = {"sinfo"}, description = "Informazioni del server", async = false)
    @Permission("hiru.info")
    public void info(@Sender Player player) {
        Config config = plugin.getConfigFile();
        String title = config.getString("info.menu-title", "&8&lServer Info");
        String serverName = config.getString("info.server-name", "Server");
        String owner = config.getString("info.owner", "?");
        String discord = config.getString("info.discord", "?");
        String website = config.getString("info.website", "?");

        GUI.Builder builder = GUI.builder(title, 3).border(Material.CYAN_STAINED_GLASS_PANE);

        ConfigurationSection slots = config.getConfigurationSection("info.slots");
        if (slots != null) {
            for (String key : slots.getKeys(false)) {
                int slot = Integer.parseInt(key);
                Material mat = Material.valueOf(config.getString("info.slots." + key + ".material", "STONE").toUpperCase());
                String name = config.getString("info.slots." + key + ".name", "");
                List<String> lore = config.getStringList("info.slots." + key + ".lore").stream()
                        .map(line -> line.replace("%server-name%", serverName)
                                .replace("%owner%", owner).replace("%discord%", discord).replace("%website%", website))
                        .collect(Collectors.toList());
                builder.item(slot, mat, name, lore);
            }
        }
        builder.open(player);
    }
}
