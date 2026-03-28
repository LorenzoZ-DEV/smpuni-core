package dev.lorenzz.hirustuffs.modules.request;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
public class RequestModule extends AbstractModule {
    private RequestManager requestManager;
    public RequestModule(Hiru plugin) {
        super(plugin, "request");
    }
    @Override
    public void onEnable() {
        requestManager = new RequestManager(plugin);
        requestManager.init();
        plugin.getCommandManager().register(new RequestCommand(requestManager));
        logger.info("[Hiru] RequestModule loaded.");
    }
    @Override
    public void onDisable() {
        super.onDisable();
    }
    public RequestManager getRequestManager() {
        return requestManager;
    }
}
