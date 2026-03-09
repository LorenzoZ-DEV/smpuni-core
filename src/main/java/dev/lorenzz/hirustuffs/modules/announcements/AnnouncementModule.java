package dev.lorenzz.hirustuffs.modules.announcements;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class AnnouncementModule extends AbstractModule {

    private AnnouncementManager announcementManager;

    public AnnouncementModule(Hiru plugin) {
        super(plugin, "announcements");
    }

    @Override
    public void onEnable() {
        announcementManager = new AnnouncementManager(plugin);
        announcementManager.load();

        plugin.getCommandManager().register(new AnnouncementCommand());

        logger.info("[Hiru] AnnouncementModule loaded.");
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
