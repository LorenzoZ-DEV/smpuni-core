package dev.lorenzz.hirustuffs.modules.enderchest;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class EnderChestModule extends AbstractModule
{
    public EnderChestModule(Hiru plugin) {
        super(plugin, "enderchest");
    }

     @Override
     public void onEnable() {
         plugin.getCommandManager().register(new EnderChestCommands());
         logger.info("[Hiru] EnderChestModule loaded.");
     }
}
