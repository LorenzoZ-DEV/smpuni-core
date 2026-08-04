package dev.lorenzz.lxsmp.modules.report;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;

public class ReportModule extends AbstractModule {

    private ReportManager reportManager;

    public ReportModule(LXSMP plugin) {
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
