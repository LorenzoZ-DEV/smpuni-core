package dev.lorenzz.hirustuffs.modules.rtp;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Config;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomTeleportService {

    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.25D;
    private static final int PRELOAD_CHUNK_RADIUS = 3;

    private final Hiru plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pendingTeleportOrigins = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> pendingCountdowns = new ConcurrentHashMap<>();

    private final Map<String, WorldRtpConfig> worldConfigs = new ConcurrentHashMap<>();

    private int maxAttempts;
    private long cooldownSeconds;
    private long delaySeconds;
    private Set<Material> unsafeBlocks;
    private String prefix;

    public record WorldRtpConfig(
            String worldName,
            int minRadius,
            int maxRadius,
            int minY,
            int maxY,
            boolean enabled
    ) {}

    public RandomTeleportService(Hiru plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        Config config = plugin.getConfigFile();
        this.prefix = config.getString("messages.prefix", "&8[&bHiru&8] ");

        this.maxAttempts = Math.max(1, config.getInt("rtp.max-attempts", 20));
        this.cooldownSeconds = Math.max(0, config.getLong("rtp.cooldown-seconds", 60));
        this.delaySeconds = Math.max(0, config.getLong("rtp.teleport-delay-seconds", 3));

        this.unsafeBlocks = EnumSet.noneOf(Material.class);
        List<String> configuredBlocks = config.getStringList("rtp.unsafe-blocks");
        for (String blockName : configuredBlocks) {
            Material material = Material.matchMaterial(blockName);
            if (material != null) {
                this.unsafeBlocks.add(material);
            } else {
                plugin.getLogger().warning("[Hiru] Ignored unknown unsafe block in rtp config: " + blockName);
            }
        }

        worldConfigs.clear();

        worldConfigs.put("world", new WorldRtpConfig(
                config.getString("rtp.worlds.overworld.world-name", "world"),
                Math.max(0, config.getInt("rtp.worlds.overworld.min-radius", 250)),
                Math.max(1, config.getInt("rtp.worlds.overworld.max-radius", 1500)),
                config.getInt("rtp.worlds.overworld.min-y", 64),
                config.getInt("rtp.worlds.overworld.max-y", 256),
                config.getBoolean("rtp.worlds.overworld.enabled", true)
        ));

        worldConfigs.put("world_nether", new WorldRtpConfig(
                config.getString("rtp.worlds.nether.world-name", "world_nether"),
                Math.max(0, config.getInt("rtp.worlds.nether.min-radius", 100)),
                Math.max(1, config.getInt("rtp.worlds.nether.max-radius", 500)),
                config.getInt("rtp.worlds.nether.min-y", 32),
                config.getInt("rtp.worlds.nether.max-y", 100),
                config.getBoolean("rtp.worlds.nether.enabled", true)
        ));

        worldConfigs.put("world_the_end", new WorldRtpConfig(
                config.getString("rtp.worlds.end.world-name", "world_the_end"),
                Math.max(0, config.getInt("rtp.worlds.end.min-radius", 500)),
                Math.max(1, config.getInt("rtp.worlds.end.max-radius", 2000)),
                config.getInt("rtp.worlds.end.min-y", 50),
                config.getInt("rtp.worlds.end.max-y", 100),
                config.getBoolean("rtp.worlds.end.enabled", true)
        ));
    }

    public void handleCommand(Player player, String targetWorldName) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetWorldName, "targetWorldName");

        WorldRtpConfig worldConfig = worldConfigs.get(targetWorldName);
        if (worldConfig == null) {
            worldConfig = worldConfigs.values().stream()
                    .filter(c -> c.worldName().equalsIgnoreCase(targetWorldName))
                    .findFirst()
                    .orElse(null);
        }

        if (worldConfig == null || !worldConfig.enabled()) {
            sendMessage(player, "rtp.messages.world-disabled", Map.of("{world}", targetWorldName));
            return;
        }

        World world = Bukkit.getWorld(worldConfig.worldName());
        if (world == null) {
            sendMessage(player, "rtp.messages.error-no-world", Map.of("{world}", targetWorldName));
            return;
        }

        if (!player.hasPermission("hiru.rtp.bypasscooldown")) {
            long remaining = getRemainingCooldown(player.getUniqueId());
            if (remaining > 0) {
                sendMessage(player, "rtp.messages.cooldown", Map.of("{time}", formatDuration(remaining)));
                return;
            }
        }

        pendingTeleportOrigins.put(player.getUniqueId(), player.getLocation().clone());

        if (delaySeconds > 0) {
            sendMessage(player, "rtp.messages.teleporting", Map.of("{seconds}", String.valueOf(delaySeconds)));
            startCountdown(player, world, worldConfig);
        } else {
            executeTeleport(player.getUniqueId(), world, worldConfig);
        }
    }

    private void startCountdown(Player player, World world, WorldRtpConfig worldConfig) {
        UUID playerId = player.getUniqueId();

        BukkitRunnable existing = pendingCountdowns.remove(playerId);
        if (existing != null) {
            existing.cancel();
        }

        BukkitRunnable countdownTask = new BukkitRunnable() {
            int countdown = (int) delaySeconds;

            @Override
            public void run() {
                Player current = Bukkit.getPlayer(playerId);
                if (current == null || !current.isOnline()) {
                    cancelPendingTeleport(playerId, "rtp.messages.cancelled");
                    pendingCountdowns.remove(playerId);
                    cancel();
                    return;
                }

                if (!ensureStationary(playerId, current)) {
                    pendingCountdowns.remove(playerId);
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    current.playSound(current.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    sendActionBar(current, "rtp.messages.actionbar-countdown", Map.of("{seconds}", String.valueOf(countdown)));
                    countdown--;
                } else {
                    pendingCountdowns.remove(playerId);
                    cancel();
                    executeTeleport(playerId, world, worldConfig);
                }
            }
        };

        pendingCountdowns.put(playerId, countdownTask);
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    public boolean isWorldEnabled(String worldName) {
        WorldRtpConfig config = worldConfigs.get(worldName);
        return config != null && config.enabled();
    }

    public WorldRtpConfig getWorldConfig(String worldName) {
        return worldConfigs.get(worldName);
    }

    public Set<String> getAvailableWorlds() {
        Set<String> worlds = new LinkedHashSet<>();
        for (Map.Entry<String, WorldRtpConfig> entry : worldConfigs.entrySet()) {
            if (entry.getValue().enabled()) {
                worlds.add(entry.getKey());
            }
        }
        return worlds;
    }

    private void executeTeleport(UUID playerId, World world, WorldRtpConfig worldConfig) {
        Player target = Bukkit.getPlayer(playerId);
        if (target == null || !target.isOnline()) {
            cancelPendingTeleport(playerId, "rtp.messages.cancelled");
            return;
        }

        if (!ensureStationary(playerId, target)) {
            return;
        }

        sendActionBar(target, "rtp.messages.actionbar-searching", Map.of());

        findSafeLocationAsync(world, worldConfig).thenAccept(location -> {
            if (location == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        cancelPendingTeleport(playerId, "rtp.messages.fail"));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player current = Bukkit.getPlayer(playerId);
                if (current == null || !current.isOnline()) {
                    cancelPendingTeleport(playerId, "rtp.messages.cancelled");
                    return;
                }
                if (!ensureStationary(playerId, current)) {
                    return;
                }

                pendingTeleportOrigins.remove(playerId);

                current.teleportAsync(location).thenAccept(success -> {
                    if (success) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            startCooldown(current.getUniqueId());
                            sendMessage(current, "rtp.messages.teleported", Map.of(
                                    "{world}", world.getName(),
                                    "{x}", String.valueOf(location.getBlockX()),
                                    "{z}", String.valueOf(location.getBlockZ())
                            ));
                            current.playSound(current.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                            sendActionBar(current, "rtp.messages.actionbar-success", Map.of(
                                    "{world}", world.getName(),
                                    "{x}", String.valueOf(location.getBlockX()),
                                    "{z}", String.valueOf(location.getBlockZ())
                            ));
                            preloadChunksAround(world, location);
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () ->
                                cancelPendingTeleport(playerId, "rtp.messages.fail"));
                    }
                });
            });
        }).exceptionally(ex -> {
            plugin.getLogger().severe("[Hiru] Failed to complete random teleport: " + ex.getMessage());
            Bukkit.getScheduler().runTask(plugin, () ->
                    cancelPendingTeleport(playerId, "rtp.messages.fail"));
            return null;
        });
    }

    private void preloadChunksAround(World world, Location center) {
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;

        for (int dx = -PRELOAD_CHUNK_RADIUS; dx <= PRELOAD_CHUNK_RADIUS; dx++) {
            for (int dz = -PRELOAD_CHUNK_RADIUS; dz <= PRELOAD_CHUNK_RADIUS; dz++) {
                world.getChunkAtAsync(centerChunkX + dx, centerChunkZ + dz);
            }
        }
    }

    private CompletableFuture<Location> findSafeLocationAsync(World world, WorldRtpConfig worldConfig) {
        CompletableFuture<Location> future = new CompletableFuture<>();
        attemptFindLocation(world, worldConfig, future, 0);
        return future;
    }

    private void attemptFindLocation(World world, WorldRtpConfig worldConfig, CompletableFuture<Location> future, int attempt) {
        if (future.isDone()) return;
        if (attempt >= maxAttempts) {
            future.complete(null);
            return;
        }

        int[] coords = generateCoordinates(world, worldConfig);
        int chunkX = coords[0] >> 4;
        int chunkZ = coords[1] >> 4;

        world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
            if (future.isDone()) return;

            Location safe = evaluateLocation(world, worldConfig, coords[0], coords[1]);
            if (safe != null) {
                future.complete(safe);
            } else {
                attemptFindLocation(world, worldConfig, future, attempt + 1);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().warning("[Hiru] Failed to load chunk at " + chunkX + ", " + chunkZ + ": " + ex.getMessage());
            attemptFindLocation(world, worldConfig, future, attempt + 1);
            return null;
        });
    }

    private Location evaluateLocation(World world, WorldRtpConfig worldConfig, int x, int z) {
        return switch (world.getEnvironment()) {
            case NETHER -> findNetherSafeLocation(world, worldConfig, x, z);
            case THE_END -> findEndSafeLocation(world, worldConfig, x, z);
            default -> findOverworldSafeLocation(world, worldConfig, x, z);
        };
    }

    private Location findOverworldSafeLocation(World world, WorldRtpConfig worldConfig, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        int minAllowedY = Math.max(worldConfig.minY(), world.getMinHeight());
        int maxAllowedY = Math.min(worldConfig.maxY(), world.getMaxHeight());

        if (highestY < minAllowedY || highestY > maxAllowedY) return null;

        Block ground = world.getBlockAt(x, highestY, z);
        if (!ground.getType().isSolid() || isUnsafe(ground.getType())) return null;

        Block feet = ground.getRelative(BlockFace.UP);
        Block head = feet.getRelative(BlockFace.UP);
        if (!feet.isPassable() || !head.isPassable() || isUnsafe(feet.getType()) || isUnsafe(head.getType())) return null;

        Location destination = ground.getLocation().add(0.5, 1.0, 0.5);
        if (!world.getWorldBorder().isInside(destination)) return null;

        return destination;
    }

    private Location findNetherSafeLocation(World world, WorldRtpConfig worldConfig, int x, int z) {
        for (int y = worldConfig.minY(); y <= worldConfig.maxY(); y++) {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            if (ground.getType().isSolid() && !isUnsafe(ground.getType()) &&
                    feet.isPassable() && !isUnsafe(feet.getType()) &&
                    head.isPassable() && !isUnsafe(head.getType())) {

                Location destination = ground.getLocation().add(0.5, 1.0, 0.5);
                if (world.getWorldBorder().isInside(destination)) return destination;
            }
        }
        return null;
    }

    private Location findEndSafeLocation(World world, WorldRtpConfig worldConfig, int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        if (highestY < worldConfig.minY()) return null;

        int searchY = Math.min(highestY, worldConfig.maxY());
        for (int y = searchY; y >= worldConfig.minY(); y--) {
            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            if (ground.getType().isSolid() && !isUnsafe(ground.getType()) &&
                    feet.isPassable() && !isUnsafe(feet.getType()) &&
                    head.isPassable() && !isUnsafe(head.getType())) {

                Location destination = ground.getLocation().add(0.5, 1.0, 0.5);
                if (world.getWorldBorder().isInside(destination)) return destination;
            }
        }
        return null;
    }

    private boolean isUnsafe(Material material) {
        return unsafeBlocks.contains(material);
    }

    private int[] generateCoordinates(World world, WorldRtpConfig worldConfig) {
        double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2);
        double minSq = (double) worldConfig.minRadius() * worldConfig.minRadius();
        double maxSq = (double) worldConfig.maxRadius() * worldConfig.maxRadius();
        double radius = Math.sqrt(ThreadLocalRandom.current().nextDouble(minSq, maxSq));

        Location center = world.getSpawnLocation();
        int x = center.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
        int z = center.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
        return new int[]{x, z};
    }

    private long getRemainingCooldown(UUID playerId) {
        Long expiresAt = cooldowns.get(playerId);
        if (expiresAt == null) return 0;
        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }
        return (long) Math.ceil(remainingMillis / 1000.0);
    }

    private void startCooldown(UUID playerId) {
        if (cooldownSeconds <= 0) {
            cooldowns.remove(playerId);
            return;
        }
        cooldowns.put(playerId, System.currentTimeMillis() + cooldownSeconds * 1000L);
    }

    private void sendMessage(UUID playerId, String path, Map<String, String> placeholders) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            sendMessage(player, path, placeholders);
        }
    }

    private void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String raw = plugin.getConfigFile().getString(path);
        if (raw == null || raw.isEmpty()) return;
        raw = raw.replace("{prefix}", prefix);
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace(entry.getKey(), entry.getValue());
            }
        }
        Messages.send(player, raw);
    }

    private void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        String raw = plugin.getConfigFile().getString(path);
        if (raw == null || raw.isEmpty()) return;
        raw = raw.replace("{prefix}", prefix);
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace(entry.getKey(), entry.getValue());
            }
        }
        player.sendActionBar(Messages.colorComponent(raw));
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "0s";
        Duration duration = Duration.ofSeconds(seconds);
        long minutes = duration.toMinutes();
        long secs = duration.minusMinutes(minutes).getSeconds();
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
    }

    private boolean ensureStationary(UUID playerId, Player player) {
        Location origin = pendingTeleportOrigins.get(playerId);
        if (origin == null) {
            cancelPendingTeleport(playerId, "rtp.messages.cancelled");
            return false;
        }
        if (hasMoved(origin, player.getLocation())) {
            cancelPendingTeleport(playerId, "rtp.messages.moved");
            return false;
        }
        return true;
    }

    private boolean hasMoved(Location origin, Location current) {
        if (origin == null || current == null) return true;
        if (origin.getWorld() == null || current.getWorld() == null) return true;
        if (!Objects.equals(origin.getWorld(), current.getWorld())) return true;
        return origin.distanceSquared(current) > MOVEMENT_TOLERANCE_SQUARED;
    }

    private void cancelPendingTeleport(UUID playerId, String messagePath) {
        cancelPendingTeleport(playerId, messagePath, Map.of());
    }

    private void cancelPendingTeleport(UUID playerId, String messagePath, Map<String, String> placeholders) {
        pendingTeleportOrigins.remove(playerId);
        BukkitRunnable countdown = pendingCountdowns.remove(playerId);
        if (countdown != null) countdown.cancel();
        if (messagePath != null) sendMessage(playerId, messagePath, placeholders);
    }
}
