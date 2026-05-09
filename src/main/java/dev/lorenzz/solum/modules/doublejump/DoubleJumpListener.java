package dev.lorenzz.solum.modules.doublejump;

import dev.lorenzz.solum.Solum;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DoubleJumpListener implements Listener {

    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();

    private volatile boolean enabled = true;
    private volatile String spawnWorld = "spawnnew";
    private volatile double power = 1.0;
    private volatile double hMul = 2.5;
    private volatile double vMul = 1.0;
    private volatile long cooldownMs = 1500;

    public DoubleJumpListener() {
        refreshConfig();
    }

    public void refreshConfig() {
        var cfg = Solum.getInstance().getConfigFile();
        enabled = cfg.getBoolean("double-jump.enabled", true);
        spawnWorld = cfg.getString("double-jump.spawn-world", "spawnnew");
        power = cfg.getDouble("double-jump.power", 1.0);
        hMul = cfg.getDouble("double-jump.horizontal-multiplier", 2.5);
        vMul = cfg.getDouble("double-jump.vertical-multiplier", 1.0);
        cooldownMs = cfg.getLong("double-jump.cooldown-ms", 1500);
    }

    private boolean isInSpawnWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(spawnWorld);
    }

    private boolean canUse(Player player) {
        if (!enabled) return false;
        if (!isInSpawnWorld(player)) return false;
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return false;
        return player.hasPermission("solum.doublejump");
    }

    private void activate(Player player) {
        if (!canUse(player)) return;
        active.add(player.getUniqueId());
        enableFlightNextTick(player);
    }

    private void enableFlightNextTick(Player player) {
        Solum.getInstance().getServer().getScheduler().runTask(Solum.getInstance(), () -> {
            if (!player.isOnline()) return;
            if (!active.contains(player.getUniqueId())) return;
            GameMode gm = player.getGameMode();
            if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;
            if (!player.getAllowFlight()) player.setAllowFlight(true);
        });
    }

    private void deactivate(Player player) {
        if (!active.remove(player.getUniqueId())) return;
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        activate(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        active.remove(event.getPlayer().getUniqueId());
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (canUse(p)) activate(p); else deactivate(p);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        GameMode gm = event.getNewGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            active.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player p = event.getPlayer();
        String respawnWorld = event.getRespawnLocation().getWorld() != null
                ? event.getRespawnLocation().getWorld().getName() : null;
        if (respawnWorld == null) return;
        if (!respawnWorld.equalsIgnoreCase(spawnWorld)) return;
        if (!enabled) return;
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;
        if (!p.hasPermission("solum.doublejump")) return;
        active.add(p.getUniqueId());
        enableFlightNextTick(p);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cooldowns.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!active.contains(uuid)) return;

        event.setCancelled(true);

        if (!player.isOnline()) return;

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && now - last < cooldownMs) return;
        cooldowns.put(uuid, now);

        double h = hMul * power;
        double v = vMul * power;
        Vector dir = player.getLocation().getDirection();
        double vx = dir.getX() * h;
        double vy = v;
        double vz = dir.getZ() * h;

        player.setVelocity(new Vector(vx, vy, vz));
        player.setFallDistance(0f);
        player.setFlying(false);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_HIT, 1.0f, 1.1f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 8, 0.2, 0.1, 0.2, 0.05);
    }

    public void cleanup() {
        active.clear();
        cooldowns.clear();
    }
}
