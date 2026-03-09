package dev.lorenzz.hirustuffs.modules.homes;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.HomeManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HomeCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "home", description = "Teleport to a home", async = false)
    @Permission("hiru.home")
    public void home(@Sender Player player, @dev.lorenzz.common.annotations.Optional @Named("name") String name) {
        HomeManager hm = HomeManager.getInstance();
        if (hm == null) { Messages.send(player, "&cHomes not available."); return; }

        if (name == null || name.isEmpty()) {
            openHomesGUI(player);
            return;
        }

        Location loc = hm.getHome(player.getUniqueId(), name);
        if (loc == null) {
            Messages.send(player, "&#FF4444Home &#FFD700" + name + " &#FF4444not found!");
            return;
        }
        player.teleportAsync(loc);
        Messages.send(player, "&#55FF55Teleported to home &#FFD700" + name);
    }

    @Command(name = "homes", description = "List your homes", async = false)
    @Permission("hiru.home")
    public void homes(@Sender Player player) {
        openHomesGUI(player);
    }

    @Command(name = "sethome", description = "Set a home", async = false)
    @Permission("hiru.home")
    public void setHome(@Sender Player player, @dev.lorenzz.common.annotations.Optional("home") @Named("name") String name) {
        HomeManager hm = HomeManager.getInstance();
        if (hm == null) { Messages.send(player, "&cHomes not available."); return; }

        if (!hm.setHome(player.getUniqueId(), name, player.getLocation())) {
            int limit = hm.getHomeLimit(player.getUniqueId());
            Messages.send(player, "&#FF4444Home limit reached! &#8C8C8C(Max: &#FFD700" + limit + "&#8C8C8C)");
            return;
        }
        Messages.send(player, "&#55FF55Home &#FFD700" + name + " &#55FF55set!");
    }

    @Command(name = "delhome", description = "Delete a home", async = false)
    @Permission("hiru.home")
    public void delHome(@Sender Player player, @Named("name") String name) {
        HomeManager hm = HomeManager.getInstance();
        if (hm == null) { Messages.send(player, "&cHomes not available."); return; }

        if (!hm.deleteHome(player.getUniqueId(), name)) {
            Messages.send(player, "&#FF4444Home &#FFD700" + name + " &#FF4444not found!");
            return;
        }
        Messages.send(player, "&#55FF55Home &#FFD700" + name + " &#55FF55deleted.");
    }

    private void openHomesGUI(Player player) {
        HomeManager hm = HomeManager.getInstance();
        Map<String, Location> playerHomes = hm.getHomes(player.getUniqueId());
        int limit = hm.getHomeLimit(player.getUniqueId());

        GUI.Builder builder = GUI.builder("&#333333&lHomes &#8C8C8C(" + playerHomes.size() + "/" + limit + ")", 3)
                .border(Material.BLACK_STAINED_GLASS_PANE);

        int slot = 10;
        for (Map.Entry<String, Location> entry : playerHomes.entrySet()) {
            String homeName = entry.getKey();
            Location loc = entry.getValue();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";

            builder.item(slot, Material.RED_BED, "&#FFD700&l" + homeName,
                    List.of(
                            "&#8C8C8CWorld: &#E0E0E0" + world,
                            "&#8C8C8CX: &#E0E0E0" + loc.getBlockX() + " &#8C8C8CY: &#E0E0E0" + loc.getBlockY() + " &#8C8C8CZ: &#E0E0E0" + loc.getBlockZ(),
                            "",
                            "&#55FF55Left-click to teleport",
                            "&#FF4444Right-click to delete"
                    ),
                    p -> {
                        p.closeInventory();
                        p.teleportAsync(loc);
                        Messages.send(p, "&#55FF55Teleported to home &#FFD700" + homeName);
                    });
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 17) break;
        }
        builder.open(player);
    }
}
