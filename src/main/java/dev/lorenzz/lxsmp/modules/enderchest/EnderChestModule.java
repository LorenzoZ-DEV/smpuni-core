package dev.lorenzz.lxsmp.modules.enderchest;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class EnderChestModule extends AbstractModule
{
    public EnderChestModule(LXSMP plugin) {
        super(plugin, "enderchest");
    }

     @Override
     public void onEnable() {
         plugin.getCommandManager().register(new EnderChestCommands());
         logger.info("[Solum] EnderChestModule loaded.");
     }
}
