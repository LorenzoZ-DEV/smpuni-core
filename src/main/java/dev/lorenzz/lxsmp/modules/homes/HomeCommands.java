package dev.lorenzz.lxsmp.modules.homes;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.HomeManager;
import dev.lorenzz.lxsmp.utils.GUI;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

@CommandAlias("home")
@CommandPermission("solum.home")
@Description("Teleport to a home")
public class HomeCommands extends BaseCommand {

    private final LXSMP plugin = LXSMP.getInstance();

    @Default
    public void home(Player player, @Optional String name) {
        HomeManager hm = HomeManager.getInstance();
        if (hm == null) { Messages.send(player, "&cHomes not available."); return; }

        if (name == null || name.isEmpty()) {
            HomeHelper.openHomesGUI(player);
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
}

@CommandAlias("homes")
@CommandPermission("solum.home")
@Description("List your homes")
class HomesCommand extends BaseCommand {
    @Default
    public void homes(Player player) {
        HomeHelper.openHomesGUI(player);
    }
}

@CommandAlias("sethome")
@CommandPermission("solum.home")
@Description("Set a home")
class SetHomeCommand extends BaseCommand {
    @Default
    public void setHome(Player player, @Optional String name) {
        if (name == null || name.isEmpty()) name = "home";
        HomeManager hm = HomeManager.getInstance();
        if (hm == null) { Messages.send(player, "&cHomes not available."); return; }

        if (!hm.setHome(player.getUniqueId(), name, player.getLocation())) {
            int limit = hm.getHomeLimit(player.getUniqueId());
            Messages.send(player, "&#FF4444Home limit reached! &#8C8C8C(Max: &#FFD700" + limit + "&#8C8C8C)");
            return;
        }
        Messages.send(player, "&#55FF55Home &#FFD700" + name + " &#55FF55set!");
    }
}

@CommandAlias("delhome")
@CommandPermission("solum.home")
@Description("Delete a home")
class DelHomeCommand extends BaseCommand {
    @Default
    public void delHome(Player player, String name) {
        HomeManager hm = HomeManager.getInstance();
        if (hm == null) { Messages.send(player, "&cHomes not available."); return; }

        if (!hm.deleteHome(player.getUniqueId(), name)) {
            Messages.send(player, "&#FF4444Home &#FFD700" + name + " &#FF4444not found!");
            return;
        }
        Messages.send(player, "&#55FF55Home &#FFD700" + name + " &#55FF55deleted.");
    }
}

class HomeHelper {

    static void openHomesGUI(Player player) {
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

