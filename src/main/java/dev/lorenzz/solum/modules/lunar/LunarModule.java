package dev.lorenzz.solum.modules.lunar;

import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.module.richpresence.RichPresenceModule;
import com.lunarclient.apollo.module.richpresence.ServerRichPresence;
import com.lunarclient.apollo.module.waypoint.Waypoint;
import com.lunarclient.apollo.module.waypoint.WaypointModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import com.lunarclient.apollo.common.location.ApolloBlockLocation;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.Optional;
import java.util.UUID;

/**
 * LunarClientSupport Module
 *
 * Provides integration with Apollo (Lunar Client API) for:
 * - Waypoints for teammates
 * - Discord Rich Presence with server name and online time
 *
 * NOTE: Requires the apollo-bukkit plugin to be installed on the server.
 */
public class LunarModule extends AbstractModule {

    private RichPresenceModule richPresenceModule;
    private WaypointModule waypointModule;

    public LunarModule(Solum plugin) {
        super(plugin, "lunar");
    }

    @Override
    public void onEnable() {
        try {
            Class.forName("com.lunarclient.apollo.Apollo");
        } catch (ClassNotFoundException e) {
            logger.warning("[Solum] Apollo (Lunar Client API) not found. LunarModule disabled.");
            return;
        }

        try {
            richPresenceModule = Apollo.getModuleManager().getModule(RichPresenceModule.class);
            waypointModule = Apollo.getModuleManager().getModule(WaypointModule.class);
            registerListener(new LunarListener(plugin, this));
            logger.info("[Solum] LunarModule loaded with Apollo API (Rich Presence + Waypoints).");
        } catch (Exception e) {
            logger.warning("[Solum] Failed to hook into Apollo API: " + e.getMessage());
        }
    }

    /**
     * Updates the Discord Rich Presence for a Lunar Client player.
     */
    public void updateRichPresence(UUID playerId) {
        if (richPresenceModule == null) return;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(playerId);
        apolloPlayer.ifPresent(player -> {
            String serverName = plugin.getConfigFile().getString("lunar.server-name", "My SMP");
            ServerRichPresence richPresence = ServerRichPresence.builder()
                    .gameName(serverName)
                    .gameVariantName("SMP")
                    .build();
            richPresenceModule.overrideServerRichPresence(player, richPresence);
        });
    }

    /**
     * Resets the Rich Presence for a player when they leave.
     */
    public void resetRichPresence(UUID playerId) {
        if (richPresenceModule == null) return;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(playerId);
        apolloPlayer.ifPresent(richPresenceModule::resetServerRichPresence);
    }

    /**
     * Sends a waypoint for a teammate to a Lunar Client player.
     */
    public void sendTeammateWaypoint(UUID viewerId, Player teammate) {
        if (waypointModule == null) return;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(viewerId);
        apolloPlayer.ifPresent(viewer -> {
            Location loc = teammate.getLocation();
            Waypoint waypoint = Waypoint.builder()
                    .name(teammate.getName())
                    .location(ApolloBlockLocation.builder()
                            .world(loc.getWorld().getName())
                            .x(loc.getBlockX())
                            .y(loc.getBlockY())
                            .z(loc.getBlockZ())
                            .build())
                    .color(Color.GREEN)
                    .preventRemoval(false)
                    .build();
            waypointModule.displayWaypoint(viewer, waypoint);
        });
    }

    /**
     * Removes a teammate waypoint from a Lunar Client player.
     */
    public void removeTeammateWaypoint(UUID viewerId, String teammateName) {
        if (waypointModule == null) return;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(viewerId);
        apolloPlayer.ifPresent(viewer -> {
            // Remove waypoint by name
            waypointModule.removeWaypoint(viewer, teammateName);
        });
    }

    /**
     * Sends waypoints for all online players to the given viewer.
     */
    public void sendAllTeammateWaypoints(UUID viewerId) {
        if (waypointModule == null) return;
        Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer == null) return;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(viewerId)) continue;
            sendTeammateWaypoint(viewerId, online);
        }
    }

    /**
     * Removes all teammate waypoints from a player.
     */
    public void removeAllTeammateWaypoints(UUID viewerId) {
        if (waypointModule == null) return;
        Optional<ApolloPlayer> apolloPlayer = Apollo.getPlayerManager().getPlayer(viewerId);
        apolloPlayer.ifPresent(waypointModule::resetWaypoints);
    }

    public WaypointModule getWaypointModule() { return waypointModule; }
    public RichPresenceModule getRichPresenceModule() { return richPresenceModule; }
}
