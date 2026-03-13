package dev.lorenzz.hirustuffs.modules.report;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;

public class ReportModule extends AbstractModule {

    private ReportManager reportManager;

    public ReportModule(Hiru plugin) {
        super(plugin, "report");
    }

    @Override
    public void onEnable() {
        reportManager = new ReportManager(plugin);
        reportManager.init();
        plugin.getCommandManager().register(new ReportCommand(reportManager));
        logger.info("[Hiru] ReportModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public ReportManager getReportManager() {
        return reportManager;
    }
}
