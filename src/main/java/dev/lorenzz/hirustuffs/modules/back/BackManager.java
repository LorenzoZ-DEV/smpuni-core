package dev.lorenzz.hirustuffs.modules.back;

import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackManager {

    private final Hiru plugin;
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    private int cooldownSeconds;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public BackManager(Hiru plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.cooldownSeconds = plugin.getConfigFile().getInt("back.cooldown-seconds", 0);
    }

    public void setDeathLocation(UUID playerId, Location location) {
        deathLocations.put(playerId, location.clone());
    }

    public Location getDeathLocation(UUID playerId) {
        return deathLocations.get(playerId);
    }

    public Location consumeDeathLocation(UUID playerId) {
        return deathLocations.remove(playerId);
    }

    public boolean hasDeathLocation(UUID playerId) {
        return deathLocations.containsKey(playerId);
    }

    public long getRemainingCooldown(UUID playerId) {
        if (cooldownSeconds <= 0) return 0;
        Long expiresAt = cooldowns.get(playerId);
        if (expiresAt == null) return 0;
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }
        return (long) Math.ceil(remaining / 1000.0);
    }

    public void startCooldown(UUID playerId) {
        if (cooldownSeconds <= 0) return;
        cooldowns.put(playerId, System.currentTimeMillis() + cooldownSeconds * 1000L);
    }

    public void removePlayer(UUID playerId) {
        deathLocations.remove(playerId);
        cooldowns.remove(playerId);
    }
}
