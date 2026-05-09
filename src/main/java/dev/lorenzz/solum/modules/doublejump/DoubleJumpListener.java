package dev.lorenzz.solum.modules.doublejump;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Double jump che NON tiene la fly attiva sui giocatori.
 *
 * Strategia: su {@link PlayerJumpEvent} (Paper, scatta solo per un salto reale
 * da terra) abilitiamo per un istante {@code allowFlight} così il client può
 * inviare un secondo "jump" mentre è in aria, che il server riceve come
 * {@link PlayerToggleFlightEvent}. Quell'evento viene cancellato, applichiamo
 * la velocity di rimbalzo e spegniamo subito {@code allowFlight}. Se il
 * giocatore atterra senza usare il secondo salto, il flag viene rimosso lo
 * stesso. Il giocatore quindi non ha mai effettivamente la fly disponibile.
 */
public class DoubleJumpListener implements Listener {

    private static final String PERMISSION = "solum.doublejump";

    private final Solum plugin;
    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    /** Giocatori "armati" per un secondo salto in aria. */
    private final Set<UUID> armed = ConcurrentHashMap.newKeySet();

    private volatile boolean cfgEnabled = true;
    private volatile String cfgSpawnWorld = "spawnnew";
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

    public void cleanup() {
        for (UUID id : armed) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()
                    && p.getGameMode() != GameMode.CREATIVE
                    && p.getGameMode() != GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
        }
        armed.clear();
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

    private void disarm(Player player) {
        armed.remove(player.getUniqueId());
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (!player.isFlying() && player.getAllowFlight()) {
            player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (!canUse(player)) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cfgCooldownMs) return;

        armed.add(player.getUniqueId());
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!event.isFlying()) return;
        if (!armed.contains(uuid)) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(uuid, 0L);
        if (now - last < cfgCooldownMs) {
            disarm(player);
            return;
        }
        cooldowns.put(uuid, now);

        Location loc = player.getLocation();
        double hMul = cfgHMul * cfgPower;
        double vMul = cfgVMul * cfgPower;
        Vector dir = loc.getDirection();
        double vx = dir.getX() * hMul;
        double vy = vMul;
        double vz = dir.getZ() * hMul;

        player.setVelocity(new Vector(vx, vy, vz));
        player.setFallDistance(0f);
        player.setFlying(false);

        armed.remove(uuid);
        player.setAllowFlight(false);

        if (loc.getWorld() != null) {
            loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_HIT, 1.0f, 1.1f);
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 8, 0.2, 0.1, 0.2, 0.05);
        }
    }

    /**
     * Rete di sicurezza: se il giocatore atterra senza consumare il secondo
     * salto, rimuoviamo comunque il flag temporaneo di fly.
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!armed.contains(player.getUniqueId())) return;
        if (!player.isOnGround()) return;
        disarm(player);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (!isInSpawnWorld(p)) {
            disarm(p);
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        GameMode gm = event.getNewGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) {
            armed.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        armed.remove(event.getPlayer().getUniqueId());
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();
        armed.remove(id);
        cooldowns.remove(id);
    }
}
