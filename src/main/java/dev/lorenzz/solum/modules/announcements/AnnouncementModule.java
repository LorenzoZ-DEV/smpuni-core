package dev.lorenzz.solum.modules.announcements;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class AnnouncementModule extends AbstractModule {

    private AnnouncementManager announcementManager;

    public AnnouncementModule(Solum plugin) {
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
