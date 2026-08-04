package dev.lorenzz.lxsmp.modules.rtp;

import dev.lorenzz.lxsmp.LXSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.entity.EntityPortalEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Redirects portal travels from configured "hub" worlds (e.g. {@code spawnnew})
 * straight into a Random Teleport in the survival worlds.
 *
 * <p>Config (under {@code rtp.portal-redirect}):
 * <pre>
 *   enabled: true
 *   from-worlds:           # worlds whose portals trigger the redirect
 *     - spawnnew
 *   nether-target: world         # rtp bucket key when entering a nether portal
 *   end-target: world_the_end    # rtp bucket key when entering an end portal
 * </pre>
 */
public final class RtpPortalListener implements Listener {

    private final LXSMP plugin;
    private final RandomTeleportService service;

    public RtpPortalListener(LXSMP plugin, RandomTeleportService service) {
        this.plugin = plugin;
        this.service = service;
    }

    private boolean isHubWorld(String worldName) {
        if (!plugin.getConfigFile().getBoolean("rtp.portal-redirect.enabled", true)) return false;
        List<String> raw = plugin.getConfigFile().getStringList("rtp.portal-redirect.from-worlds");
        if (raw.isEmpty()) raw = List.of("spawnnew");
        Set<String> hubs = new HashSet<>();
        for (String s : raw) hubs.add(s.toLowerCase(Locale.ROOT));
        return hubs.contains(worldName.toLowerCase(Locale.ROOT));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getFrom() == null || event.getFrom().getWorld() == null) return;
        String fromWorld = event.getFrom().getWorld().getName();
        if (!isHubWorld(fromWorld)) return;

        String target;
        switch (event.getCause()) {
            case NETHER_PORTAL -> target = plugin.getConfigFile()
                    .getString("rtp.portal-redirect.nether-target", "world");
            case END_PORTAL, END_GATEWAY -> target = plugin.getConfigFile()
                    .getString("rtp.portal-redirect.end-target", "world_the_end");
            default -> { return; }
        }

        event.setCancelled(true);
        // Run on next tick so the portal interaction is fully resolved before we teleport.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                service.handleCommand(event.getPlayer(), target);
            }
        });
    }

    /** Vanilla mobs / vehicles entering portals shouldn't get vacuumed up; just cancel. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.getFrom() == null || event.getFrom().getWorld() == null) return;
        if (isHubWorld(event.getFrom().getWorld().getName())) {
            event.setCancelled(true);
        }
    }
}

