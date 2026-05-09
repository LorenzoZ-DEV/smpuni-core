package dev.lorenzz.solum.modules.enderchest;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class EnderChestModule extends AbstractModule
{
    public EnderChestModule(Solum plugin) {
        super(plugin, "enderchest");
    }

     @Override
     public void onEnable() {
         plugin.getCommandManager().register(new EnderChestCommands());
         logger.info("[Solum] EnderChestModule loaded.");
     }
}
