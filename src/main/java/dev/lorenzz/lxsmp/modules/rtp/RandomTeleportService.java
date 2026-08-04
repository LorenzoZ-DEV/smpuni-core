package dev.lorenzz.lxsmp.modules.rtp;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Config;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class RandomTeleportService {

    public record RTPRegion(String worldName, int minRadius, int maxRadius,
                            int minY, int maxY, boolean enabled) {}

    private record RTPRequest(UUID playerId, String worldKey) {}

    private static final String BYPASS_PERMISSION = "solum.rtp.bypass";

    private final LXSMP plugin;
    private final Queue<RTPRequest> queue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private final EnumSet<Material> unsafePassthrough;
    private final EnumSet<Material> liquidBlocks;

    private volatile int cooldownSeconds;
    private volatile int teleportDelay;
    private volatile int maxAttempts;
    private volatile int prechunkRadius;

    /** Keyed by world-name (es. "world", "world_nether"). */
    private volatile Map<String, RTPRegion> regions = Map.of();
    private volatile EnumSet<Material> blacklistedBlocks = EnumSet.noneOf(Material.class);

    public RandomTeleportService(LXSMP plugin) {
        this.plugin = plugin;
        this.unsafePassthrough = EnumSet.noneOf(Material.class);
        this.liquidBlocks = EnumSet.noneOf(Material.class);

        for (Material mat : Material.values()) {
            String matName = mat.name();
            if (matName.contains("LAVA") || matName.contains("WATER")) {
                liquidBlocks.add(mat);
            }
            if (mat.isAir() || mat.isSolid()) continue;
            if (matName.contains("LAVA") || matName.contains("WATER") || matName.contains("FIRE")
                    || matName.contains("KELP") || matName.contains("SEAGRASS")
                    || matName.contains("BUBBLE_COLUMN") || matName.contains("CORAL")) {
                unsafePassthrough.add(mat);
            }
        }

        loadConfig();
    }

    public LXSMP getPlugin() {
        return plugin;
    }

    public Map<String, RTPRegion> getRegions() {
        return regions;
    }

    public void reload() {
        loadConfig();
    }

    public void shutdown() {
        queue.clear();
        cooldowns.clear();
    }

    private void loadConfig() {
        Config config = plugin.getConfigFile();

        cooldownSeconds = config.getInt("rtp.cooldown-seconds", 300);
        teleportDelay = config.getInt("rtp.teleport-delay-seconds", 5);
        maxAttempts = config.getInt("rtp.max-attempts", 20);
        prechunkRadius = config.getInt("rtp.prechunk-radius", 3);

        regions = loadRegions(config.getConfigurationSection("rtp.worlds"));
        blacklistedBlocks = loadBlacklistedBlocks(config.getStringList("rtp.unsafe-blocks"));
    }

    private Map<String, RTPRegion> loadRegions(ConfigurationSection section) {
        if (section == null) return Map.of();

        Map<String, RTPRegion> regionMap = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection cfg = section.getConfigurationSection(key);
            if (cfg == null) continue;

            String worldName = cfg.getString("world-name", key);
            RTPRegion region = new RTPRegion(
                    worldName,
                    cfg.getInt("min-radius", 250),
                    cfg.getInt("max-radius", 5000),
                    cfg.getInt("min-y", 64),
                    cfg.getInt("max-y", 256),
                    cfg.getBoolean("enabled", true)
            );

            regionMap.put(key.toLowerCase(), region);
            if (worldName != null) {
                regionMap.putIfAbsent(worldName.toLowerCase(), region);
            }
        }
        return Map.copyOf(regionMap);
    }

    private EnumSet<Material> loadBlacklistedBlocks(List<String> names) {
        EnumSet<Material> blocks = EnumSet.noneOf(Material.class);
        for (String blockName : names) {
            try {
                blocks.add(Material.valueOf(blockName));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return blocks;
    }

    public void handleCommand(Player player, String worldKey) {
        addToQueue(player, worldKey);
    }

    public boolean isOnCooldown(Player player) {
        if (player.hasPermission(BYPASS_PERMISSION)) return false;

        Long expiry = cooldowns.get(player.getUniqueId());
        if (expiry == null) return false;

        if (expiry <= System.currentTimeMillis()) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public int getRemainingCooldown(Player player) {
        Long expiry = cooldowns.get(player.getUniqueId());
        if (expiry == null) return 0;

        long remaining = expiry - System.currentTimeMillis();
        return remaining <= 0 ? 0 : (int) (remaining / 1000L);
    }

    public void addToQueue(Player player, String worldKey) {
        if (isOnCooldown(player)) {
            sendMessage(player, "cooldown", Map.of("{time}", String.valueOf(getRemainingCooldown(player))));
            return;
        }

        RTPRegion region = resolveRegion(worldKey);
        World world = region != null ? Bukkit.getWorld(region.worldName()) : null;

        if (region == null || world == null) {
            sendMessage(player, "error-no-world", Map.of("{world}", worldKey));
            return;
        }

        if (!region.enabled()) {
            sendMessage(player, "world-disabled", Map.of("{world}", world.getName()));
            return;
        }

        boolean wasEmpty = queue.isEmpty();
        queue.add(new RTPRequest(player.getUniqueId(), worldKey));

        if (wasEmpty) processQueue();
    }

    private RTPRegion resolveRegion(String worldKey) {
        if (worldKey == null) return null;
        return regions.get(worldKey.toLowerCase());
    }

    private void processQueue() {
        while (true) {
            if (queue.isEmpty()) return;

            RTPRequest request = queue.poll();
            if (request == null) return;

            Player player = Bukkit.getPlayer(request.playerId());
            if (player == null || !player.isOnline()) continue;

            RTPRegion region = resolveRegion(request.worldKey());
            if (region == null) continue;

            World world = Bukkit.getWorld(region.worldName());
            if (world == null) {
                sendMessage(player, "error-no-world", Map.of("{world}", region.worldName()));
                continue;
            }

            sendMessage(player, "actionbar-searching", Map.of());
            findSafeLocation(world, region).thenAccept(location -> runSync(() -> {
                if (!player.isOnline()) {
                    processQueue();
                    return;
                }
                if (location == null) {
                    sendMessage(player, "fail", Map.of());
                    processQueue();
                    return;
                }

                CompletableFuture<Void> preload = preloadChunks(location);
                if (player.hasPermission(BYPASS_PERMISSION) || teleportDelay <= 0) {
                    executeTeleport(player, location, world.getName(), preload);
                } else {
                    startCountdown(player, location, world.getName(), preload);
                }
            }));
            return;
        }
    }

    private void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public CompletableFuture<Void> preloadChunks(Location location) {
        if (location == null || location.getWorld() == null || prechunkRadius <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        World world = location.getWorld();
        int centerX = location.getBlockX() >> 4;
        int centerZ = location.getBlockZ() >> 4;
        int diameter = prechunkRadius * 2 + 1;

        List<CompletableFuture<Chunk>> futures = new ArrayList<>(diameter * diameter);
        for (int dx = -prechunkRadius; dx <= prechunkRadius; dx++) {
            for (int dz = -prechunkRadius; dz <= prechunkRadius; dz++) {
                futures.add(world.getChunkAtAsync(centerX + dx, centerZ + dz));
            }
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(ex -> null);
    }

    private void startCountdown(Player player, Location destination, String worldName,
                                CompletableFuture<Void> preload) {
        Location start = player.getLocation();
        double startX = start.getX();
        double startY = start.getY();
        double startZ = start.getZ();

        new BukkitRunnable() {
            int remaining = teleportDelay;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    processQueue();
                    return;
                }

                Location current = player.getLocation();
                double dx = current.getX() - startX;
                double dy = current.getY() - startY;
                double dz = current.getZ() - startZ;
                if (dx * dx + dy * dy + dz * dz > 0.25) {
                    sendMessage(player, "moved", Map.of());
                    cancel();
                    processQueue();
                    return;
                }

                if (remaining <= 0) {
                    cancel();
                    executeTeleport(player, destination, worldName, preload);
                    return;
                }

                sendMessage(player, "teleporting", Map.of("{seconds}", String.valueOf(remaining)));
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void executeTeleport(Player player, Location location, String worldName,
                                 CompletableFuture<Void> preload) {
        preload.thenRun(() -> runSync(() -> {
            if (!player.isOnline()) {
                processQueue();
                return;
            }

            player.teleportAsync(location).thenAccept(success -> runSync(() -> {
                if (!success || !player.isOnline()) {
                    processQueue();
                    return;
                }

                player.setFallDistance(0.0f);

                long durationMs = cooldownSeconds * 1000L;
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);

                String world = worldName != null ? worldName : "";
                sendMessage(player, "teleported", Map.of(
                        "{world}", world,
                        "{x}", String.valueOf(location.getBlockX()),
                        "{y}", String.valueOf(location.getBlockY()),
                        "{z}", String.valueOf(location.getBlockZ())
                ));

                processQueue();
            }));
        }));
    }

    private void sendMessage(Player player, String key, Map<String, String> placeholders) {
        String raw = Messages.get("rtp.messages." + key);
        if (raw == null || raw.isEmpty() || raw.equals("rtp.messages." + key)) return;

        String message = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        Messages.sendNoPrefix(player, message);
    }

    public CompletableFuture<Location> findSafeLocation(World world, RTPRegion region) {
        CompletableFuture<Location> result = new CompletableFuture<>();

        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        double borderRadius = border.getSize() / 2.0 - 1.0;

        int minRadius = Math.max(1, region.minRadius());
        int maxRadius = Math.max(minRadius + 1, region.maxRadius());

        int minX = (int) Math.max(borderCenter.getX() - borderRadius, borderCenter.getX() - maxRadius);
        int maxX = (int) Math.min(borderCenter.getX() + borderRadius, borderCenter.getX() + maxRadius);
        int minZ = (int) Math.max(borderCenter.getZ() - borderRadius, borderCenter.getZ() - maxRadius);
        int maxZ = (int) Math.min(borderCenter.getZ() + borderRadius, borderCenter.getZ() + maxRadius);

        if (minX >= maxX || minZ >= maxZ) {
            result.complete(null);
            return result;
        }

        int minHeight = Math.max(world.getMinHeight(), region.minY());
        int maxHeight = Math.min(world.getMaxHeight() - 1, region.maxY());

        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
        boolean isEnd = world.getEnvironment() == World.Environment.THE_END;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int[][] coords = new int[maxAttempts][2];
        for (int i = 0; i < maxAttempts; i++) {
            int x;
            int z;
            int tries = 0;
            do {
                x = rng.nextInt(minX, maxX + 1);
                z = rng.nextInt(minZ, maxZ + 1);
                double distSq = (x - borderCenter.getX()) * (x - borderCenter.getX())
                        + (z - borderCenter.getZ()) * (z - borderCenter.getZ());
                if (distSq >= (double) minRadius * minRadius) break;
                tries++;
            } while (tries < 5);
            coords[i][0] = x;
            coords[i][1] = z;
        }

        tryLocation(world, coords, 0, isNether, isEnd, minHeight, maxHeight, result);
        return result;
    }

    private void tryLocation(World world, int[][] coords, int idx, boolean isNether,
                             boolean isEnd, int minHeight, int maxHeight,
                             CompletableFuture<Location> result) {
        if (idx >= coords.length) {
            result.complete(null);
            return;
        }

        int x = coords[idx][0];
        int z = coords[idx][1];

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
            CompletableFuture.runAsync(() -> {
                int localX = x & 0xF;
                int localZ = z & 0xF;
                try {
                    Location safe = checkSafe(world, snapshot, x, z, localX, localZ,
                            isNether, isEnd, minHeight, maxHeight);
                    if (safe != null) {
                        result.complete(safe);
                        return;
                    }
                } catch (Exception ignored) {
                }
                tryLocation(world, coords, idx + 1, isNether, isEnd, minHeight, maxHeight, result);
            });
        }).exceptionally(ex -> {
            tryLocation(world, coords, idx + 1, isNether, isEnd, minHeight, maxHeight, result);
            return null;
        });
    }

    private Location checkSafe(World world, ChunkSnapshot snapshot, int x, int z,
                               int localX, int localZ, boolean isNether, boolean isEnd,
                               int minHeight, int maxHeight) {
        if (isNether) return checkNetherSafe(world, snapshot, x, z, localX, localZ, minHeight, maxHeight);
        if (isEnd) return checkEndSafe(world, snapshot, x, z, localX, localZ, minHeight, maxHeight);
        return checkOverworldSafe(world, snapshot, x, z, localX, localZ, minHeight, maxHeight);
    }

    private Location checkOverworldSafe(World world, ChunkSnapshot snapshot, int x, int z,
                                        int localX, int localZ, int minHeight, int maxHeight) {
        int topY = Math.min(snapshot.getHighestBlockYAt(localX, localZ), maxHeight);
        if (topY <= minHeight) return null;

        for (int y = topY; y > minHeight; y--) {
            Location loc = trySpot(world, snapshot, x, z, localX, localZ, y);
            if (loc != null) return loc;
        }
        return null;
    }

    private Location checkNetherSafe(World world, ChunkSnapshot snapshot, int x, int z,
                                     int localX, int localZ, int minHeight, int maxHeight) {
        int from = Math.min(maxHeight, 120);
        int to = Math.max(minHeight, 5);
        return scanRange(world, snapshot, x, z, localX, localZ, from, to);
    }

    private Location checkEndSafe(World world, ChunkSnapshot snapshot, int x, int z,
                                  int localX, int localZ, int minHeight, int maxHeight) {
        int from = Math.min(maxHeight, 120);
        int to = Math.max(minHeight, 1);
        for (int y = from; y >= to; y--) {
            Location loc = trySpot(world, snapshot, x, z, localX, localZ, y);
            if (loc != null && hasGroundBelow(snapshot, localX, localZ, y - 1)) return loc;
        }
        return null;
    }

    private Location scanRange(World world, ChunkSnapshot snapshot, int x, int z,
                               int localX, int localZ, int fromY, int toY) {
        for (int y = fromY; y >= toY; y--) {
            Location loc = trySpot(world, snapshot, x, z, localX, localZ, y);
            if (loc != null) return loc;
        }
        return null;
    }

    private Location trySpot(World world, ChunkSnapshot snapshot, int x, int z,
                             int localX, int localZ, int y) {
        Material ground = snapshot.getBlockType(localX, y, localZ);
        if (!ground.isSolid() || liquidBlocks.contains(ground) || blacklistedBlocks.contains(ground)) {
            return null;
        }

        Material feet = snapshot.getBlockType(localX, y + 1, localZ);
        if (!isSafe(feet) || blacklistedBlocks.contains(feet)) return null;

        Material head = snapshot.getBlockType(localX, y + 2, localZ);
        if (!isSafe(head) || blacklistedBlocks.contains(head)) return null;

        Material above = snapshot.getBlockType(localX, y + 3, localZ);
        if (liquidBlocks.contains(above) || unsafePassthrough.contains(above)) return null;

        return new Location(world, x + 0.5, y + 1.0, z + 0.5);
    }

    private boolean hasGroundBelow(ChunkSnapshot snapshot, int localX, int localZ, int startY) {
        int count = 0;
        for (int y = startY; y >= Math.max(0, startY - 4); y--) {
            if (snapshot.getBlockType(localX, y, localZ).isSolid()) count++;
        }
        return count >= 2;
    }

    private boolean isSafe(Material mat) {
        return mat.isAir() || (!mat.isSolid() && !unsafePassthrough.contains(mat));
    }
}
