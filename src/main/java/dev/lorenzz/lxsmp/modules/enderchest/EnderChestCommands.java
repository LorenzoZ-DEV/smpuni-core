package dev.lorenzz.lxsmp.modules.enderchest;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

@CommandAlias("enderchest|ec")
@CommandPermission("solum.enderchest")
@Description("Open your ender chest")
public class EnderChestCommands extends BaseCommand {

    private Sound getSound(String key, Sound fallback) {
        try {
            return Sound.valueOf(LXSMP.getInstance().getConfigFile().getString("sounds." + key, fallback.name()));
        } catch (Exception e) {
            return fallback;
        }
    }

    @Default
    public void enderchest(Player player, @Optional Player target) {
        Bukkit.getScheduler().runTask(LXSMP.getInstance(), () -> {
            if (target == null) {
                player.playSound(player.getLocation(), getSound("enderchest-open", Sound.BLOCK_ENDER_CHEST_OPEN), 1.0f, 1.5f);
                player.openInventory(player.getEnderChest());
                return;
            }
            if (!player.hasPermission("solum.enderchest.others")) {
                player.playSound(player.getLocation(), getSound("error", Sound.ENTITY_VILLAGER_NO), 1.0f, 1.0f);
                player.sendMessage("&#FF4444You don't have permission to open other players' ender chests.");
                return;
            }
            player.openInventory(target.getEnderChest());
        });
    }
}

