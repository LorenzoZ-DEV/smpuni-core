package dev.lorenzz.solum.modules.doublejump;

import dev.lorenzz.solum.Solum;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DoubleJumpListener implements Listener {

    private static final String PERMISSION = "solum.doublejump";

    private final Solum plugin;
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> active = ConcurrentHashMap.newKeySet();

    private volatile boolean cfgEnabled = true;
    private volatile String cfgSpawnWorld = "spawn";
    private volatile double cfgPower = 1.0;
    private volatile double cfgHMul = 2.5;
    private volatile double cfgVMul = 1.0;
    private volatile long cfgCooldownMs = 1500L;

    public DoubleJumpListener() {
        this.plugin = Solum.getInstance();
        refreshConfig();
    }

    public void refreshConfig() {
        FileConfiguration cfg = plugin.getConfig();
        cfgEnabled = cfg.getBoolean("double-jump.enabled");
        cfgSpawnWorld = cfg.getString("double-jump.spawn-world", cfg.getString("spawn-world"));
        cfgPower = cfg.getDouble("double-jump.power", 1.0);
        cfgHMul = cfg.getDouble("double-jump.horizontal-multiplier", 2.5);
        cfgVMul = cfg.getDouble("double-jump.vertical-multiplier", 1.0);
        cfgCooldownMs = cfg.getLong("double-jump.cooldown-ms", 1500L);
    }

    public void bootstrap(Player player) {
        activate(player);
    }

    public void cleanup() {
        for (UUID id : active) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()
                    && p.getGameMode() != GameMode.CREATIVE
                    && p.getGameMode() != GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }
        active.clear();
        cooldowns.clear();
    }

    private boolean isInSpawnWorld(Player player) {
        return player.getWorld().getName().equalsIgnoreCase(cfgSpawnWorld);
    }

    private boolean canUse(Player player) {
        if (!cfgEnabled) return false;
        if (!isInSpawnWorld(player)) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return false;
        return player.hasPermission(PERMISSION);
    }

    private void activate(Player player) {
        if (!canUse(player)) return;
        active.add(player.getUniqueId());
        enableFlightNextTick(player);
    }

    private void enableFlightNextTick(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (!active.contains(player.getUniqueId())) return;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
            if (!player.getAllowFlight()) player.setAllowFlight(true);
        });
    }

    private void deactivate(Player player) {
        if (!active.remove(player.getUniqueId())) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
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
        if (canUse(p)) activate(p);
        else deactivate(p);
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
        if (event.getRespawnLocation().getWorld() == null) return;
        String respawnWorld = event.getRespawnLocation().getWorld().getName();
        if (!respawnWorld.equalsIgnoreCase(cfgSpawnWorld)) return;
        if (!cfgEnabled) return;
        if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) return;
        if (!p.hasPermission(PERMISSION)) return;
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


        if (!event.isFlying()) return;

        if (player.isOnGround()) return;

        if (!active.contains(uuid)) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(uuid, 0L);
        if (now - last < cfgCooldownMs) return;
        cooldowns.put(uuid, now);

        Location loc = player.getLocation();
        double hMul = cfgHMul * cfgPower;
        double vMul = cfgVMul * cfgPower;
        Vector dir = loc.getDirection();
        double vx = dir.getX() * hMul;
        double vy = 1.0 * vMul;
        double vz = dir.getZ() * hMul;

        player.setVelocity(new Vector(vx, vy, vz));
        player.setFallDistance(0f);
        player.setFlying(false);

        if (loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_HIT, 1.0f, 1.1f);
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 8, 0.2, 0.1, 0.2, 0.05);
        }
    }
}
