package dev.lorenzz.lxsmp.modules.announcements;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class AnnouncementModule extends AbstractModule {

    private AnnouncementManager announcementManager;

    public AnnouncementModule(LXSMP plugin) {
        super(plugin, "announcements");
    }

    @Override
    public void onEnable() {
        announcementManager = new AnnouncementManager(plugin);
        announcementManager.load();

        plugin.getCommandManager().register(new AnnouncementCommand());

        logger.info("[Solum] AnnouncementModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (announcementManager != null) {
            announcementManager.shutdown();
        }
    }

    public AnnouncementManager getAnnouncementManager() { return announcementManager; }
}
