package dev.lorenzz.solum.modules.request;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
public class RequestModule extends AbstractModule {
    private RequestManager requestManager;
    public RequestModule(Solum plugin) {
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
