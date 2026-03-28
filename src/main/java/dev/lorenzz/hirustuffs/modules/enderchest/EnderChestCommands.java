package dev.lorenzz.hirustuffs.modules.enderchest;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EnderChestCommands
{
    @Permission("hiru.enderchest")
    @Command(name = "enderchest", aliases = "ec", description = "Open your ender chest", usage = "/enderchest", async = false)
    public void enderchest(@Sender Player player , Player target)
    {
        // openInventory MUST run on the main thread (Paper enforces this)
        Bukkit.getScheduler().runTask(Hiru.getInstance(), () -> {
            if(target == null)
            {
                Sound sound = Sound.valueOf("ENTITY_ENDER_CHEST_OPEN");
                player.playSound(player.getLocation(), sound, 1.0f, 1.5f);
                player.openInventory(player.getEnderChest());
                return;
            }
            if(!player.hasPermission("hiru.enderchest.others"))
            {
                Sound sound = Sound.valueOf("ENTITY_VILLAGER_NO");
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
                player.sendMessage("&#FF4444You don't have permission to open other players' ender chests.");
                return;
            }
            player.openInventory(target.getEnderChest());
        });
    }
}
