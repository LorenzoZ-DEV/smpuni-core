package dev.lorenzz.lxsmp.modules.request;
import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.AbstractModule;
public class RequestModule extends AbstractModule {
    private RequestManager requestManager;
    public RequestModule(LXSMP plugin) {
        super(plugin, "request");
    }
    @Override
    public void onEnable() {
        requestManager = new RequestManager(plugin);
        requestManager.init();
        plugin.getCommandManager().register(new RequestCommand(requestManager));
        logger.info("[Solum] RequestModule loaded.");
    }
    @Override
    public void onDisable() {
        super.onDisable();
    }
    public RequestManager getRequestManager() {
        return requestManager;
    }
}
