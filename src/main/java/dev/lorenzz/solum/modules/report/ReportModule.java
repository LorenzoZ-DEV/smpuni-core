package dev.lorenzz.solum.modules.report;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;

public class ReportModule extends AbstractModule {

    private ReportManager reportManager;

    public ReportModule(Solum plugin) {
        super(plugin, "report");
    }

    @Override
    public void onEnable() {
        reportManager = new ReportManager(plugin);
        reportManager.init();
        plugin.getCommandManager().register(new ReportCommand(reportManager));
        logger.info("[Solum] ReportModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public ReportManager getReportManager() {
        return reportManager;
    }
}
