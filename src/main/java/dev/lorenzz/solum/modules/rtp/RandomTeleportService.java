package dev.lorenzz.solum.modules.rtp;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Config;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class RandomTeleportService {

    public record RTPRegion(String worldName, int minX, int maxX, int minZ, int maxZ) {}

    private record SoundEntry(Sound sound, float volume, float pitch) {}

    private record RTPRequest(UUID playerId, String regionKey) {}

    private record EffectEntry(PotionEffectType type, int amplifier, int durationTicks) {}

    private record CooldownPerm(int seconds, String permission) {}

    private static final String BYPASS_PERMISSION = "solum.rtp.bypass";

    private final Solum plugin;
    private final Queue<RTPRequest> queue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    private final EnumSet<Material> unsafePassthrough;
    private final EnumSet<Material> liquidBlocks;

    private volatile int cooldownSeconds;
    private volatile int teleportDelay;
    private volatile int maxAttempts;
    private volatile int prechunkRadius;
    private volatile Map<String, RTPRegion> regions = Map.of();
    private volatile EnumSet<Material> blacklistedBlocks = EnumSet.noneOf(Material.class);
    private volatile Set<Biome> blacklistedBiomes = Set.of();
    private volatile List<EffectEntry> effects = List.of();
    private volatile List<CooldownPerm> cooldownPermissions = List.of();
    private volatile Map<String, SoundEntry> sounds = Map.of();

    public RandomTeleportService(Solum plugin) {
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

    public Map<String, RTPRegion> getRegions() {
        return regions;
    }

    public Solum getPlugin() {
        return plugin;
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

        cooldownSeconds = config.getInt("Rtp.cooldown", 60);
        teleportDelay = config.getInt("Rtp.teleport-delay", 5);
        maxAttempts = config.getInt("Rtp.max-attempts", 15);
        prechunkRadius = config.getInt("Rtp.prechunk-radius", 3);

        regions = loadRegions(config.getConfigurationSection("Rtp.regions"));
        blacklistedBlocks = loadBlacklistedBlocks(config.getStringList("Rtp.blacklisted-blocks"));
        blacklistedBiomes = loadBlacklistedBiomes(config.getStringList("Rtp.blacklisted-biomes"));
        effects = loadEffects(config.getMapList("Rtp.effects"));
        cooldownPermissions = loadCooldownPermissions(config.getConfigurationSection("Rtp.cooldown-permissions"));
        sounds = loadSounds(config.getConfigurationSection("Rtp.sounds"));
    }

    private Map<String, RTPRegion> loadRegions(ConfigurationSection section) {
        if (section == null) return Map.of();

        Map<String, RTPRegion> regionMap = new HashMap<>();
        for (String key : section.getKeys(false)) {
            regionMap.put(key, new RTPRegion(
                    section.getString(key + ".world", "world"),
                    section.getInt(key + ".min-x", -5000),
                    section.getInt(key + ".max-x", 5000),
                    section.getInt(key + ".min-z", -5000),
                    section.getInt(key + ".max-z", 5000)
            ));
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

    @SuppressWarnings("deprecation")
    private Set<Biome> loadBlacklistedBiomes(List<String> names) {
        Set<Biome> biomeSet = new HashSet<>();
        for (String biomeName : names) {
            try {
                Biome biome = Registry.BIOME.get(NamespacedKey.minecraft(biomeName.toLowerCase()));
                if (biome != null) biomeSet.add(biome);
            } catch (Exception ignored) {
            }
        }
        return Set.copyOf(biomeSet);
    }

    private List<EffectEntry> loadEffects(List<Map<?, ?>> mapList) {
        List<EffectEntry> effectList = new ArrayList<>();
        for (Map<?, ?> map : mapList) {
            Object typeObj = map.get("type");
            if (typeObj == null) continue;

            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(
                    NamespacedKey.minecraft(typeObj.toString().toLowerCase()));
            if (type == null) continue;

            Object ampObj = map.get("amplifier");
            int amplifier = ampObj instanceof Number number ? number.intValue() : 0;

            Object durObj = map.get("duration");
            int duration = (durObj instanceof Number number ? number.intValue() : 10) * 20;

            effectList.add(new EffectEntry(type, amplifier, duration));
        }
        return List.copyOf(effectList);
    }

    private List<CooldownPerm> loadCooldownPermissions(ConfigurationSection section) {
        if (section == null) return List.of();

        List<CooldownPerm> permList = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            String perm = section.getString(key);
            if (perm == null) continue;
            try {
                permList.add(new CooldownPerm(Integer.parseInt(key), perm));
            } catch (NumberFormatException ignored) {
            }
        }
        permList.sort(Comparator.comparingInt(CooldownPerm::seconds));
        return List.copyOf(permList);
    }

    private Map<String, SoundEntry> loadSounds(ConfigurationSection section) {
        if (section == null) return Map.of();

        Map<String, SoundEntry> soundMap = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection soundCfg = section.getConfigurationSection(key);
            if (soundCfg == null) continue;

            String typeName = soundCfg.getString("type", "");
            if (typeName == null || typeName.isEmpty()) continue;

            Sound snd = Registry.SOUNDS.get(NamespacedKey.minecraft(typeName.toLowerCase()));
            if (snd == null) continue;

            float volume = (float) soundCfg.getDouble("volume", 1.0);
            float pitch = (float) soundCfg.getDouble("pitch", 1.0);
            soundMap.put(key, new SoundEntry(snd, volume, pitch));
        }
        return Map.copyOf(soundMap);
    }

    public void playSound(Player player, String key) {
        SoundEntry entry = sounds.get(key);
        if (entry == null) return;
        player.playSound(player.getLocation(), entry.sound(), entry.volume(), entry.pitch());
    }

    public void handleCommand(Player player, String regionKey) {
        addToQueue(player, regionKey);
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

    public void addToQueue(Player player, String regionKey) {
        if (isOnCooldown(player)) {
            Messages.send(player, Messages.get("Rtp.cooldown")
                    .replace("{time}", String.valueOf(getRemainingCooldown(player))));
            return;
        }

        RTPRegion region = regions.get(regionKey);
        World world = region != null ? Bukkit.getWorld(region.worldName()) : null;
        if (region == null || world == null) {
            Messages.send(player, Messages.get("Rtp.world-not-found"));
            return;
        }

        boolean wasEmpty = queue.isEmpty();
        queue.add(new RTPRequest(player.getUniqueId(), regionKey));

        if (!wasEmpty) {
            Messages.send(player, Messages.get("Rtp.queue-added")
                    .replace("{position}", String.valueOf(queue.size())));
        }

        if (wasEmpty) processQueue();
    }

    private void processQueue() {
        while (true) {
            if (queue.isEmpty()) return;

            RTPRequest request = queue.poll();
            if (request == null) return;

            Player player = Bukkit.getPlayer(request.playerId());
            if (player == null || !player.isOnline()) continue;

            RTPRegion region = regions.get(request.regionKey());
            if (region == null) continue;

            World world = Bukkit.getWorld(region.worldName());
            if (world == null) {
                Messages.send(player, Messages.get("Rtp.world-not-found"));
                continue;
            }

            Messages.send(player, Messages.get("Rtp.searching"));
            findSafeLocation(world, region).thenAccept(location -> runSync(() -> {
                if (!player.isOnline()) {
                    processQueue();
                    return;
                }
                if (location == null) {
                    Messages.send(player, Messages.get("Rtp.no-safe-location"));
                    playSound(player, "no-safe-location");
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
                    Messages.send(player, Messages.get("Rtp.teleport-canceled"));
                    playSound(player, "teleport-canceled");
                    cancel();
                    processQueue();
                    return;
                }

                if (remaining <= 0) {
                    cancel();
                    executeTeleport(player, destination, worldName, preload);
                    return;
                }

                Messages.send(player, Messages.get("Rtp.delay-chat")
                        .replace("{time}", String.valueOf(remaining)));
                playSound(player, "countdown-tick");
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
                playSound(player, "teleport");
                applyEffects(player);

                long durationMs = getCooldownForPlayer(player) * 1000L;
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);

                String world = worldName != null ? worldName : "";
                Messages.send(player, Messages.get("Rtp.teleported")
                        .replace("{world}", world)
                        .replace("{x}", String.valueOf(location.getBlockX()))
                        .replace("{y}", String.valueOf(location.getBlockY()))
                        .replace("{z}", String.valueOf(location.getBlockZ())));

                processQueue();
            }));
        }));
    }

    private int getCooldownForPlayer(Player player) {
        for (CooldownPerm perm : cooldownPermissions) {
            if (player.hasPermission(perm.permission())) return perm.seconds();
        }
        return cooldownSeconds;
    }

    private void applyEffects(Player player) {
        for (EffectEntry effect : effects) {
            player.addPotionEffect(new PotionEffect(
                    effect.type(), effect.durationTicks(), effect.amplifier(), false, true, true));
        }
    }

    public CompletableFuture<Location> findSafeLocation(World world, RTPRegion region) {
        CompletableFuture<Location> result = new CompletableFuture<>();

        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        double borderRadius = border.getSize() / 2.0 - 1.0;

        int minX = Math.max(region.minX(), (int) (borderCenter.getX() - borderRadius));
        int maxX = Math.min(region.maxX(), (int) (borderCenter.getX() + borderRadius));
        int minZ = Math.max(region.minZ(), (int) (borderCenter.getZ() - borderRadius));
        int maxZ = Math.min(region.maxZ(), (int) (borderCenter.getZ() + borderRadius));

        if (minX >= maxX || minZ >= maxZ) {
            result.complete(null);
            return result;
        }

        boolean isNether = world.getEnvironment() == World.Environment.NETHER;
        boolean isEnd = world.getEnvironment() == World.Environment.THE_END;
        boolean checkBiomes = !blacklistedBiomes.isEmpty();
        int minHeight = world.getMinHeight();

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int[][] coords = new int[maxAttempts][2];
        for (int i = 0; i < maxAttempts; i++) {
            coords[i][0] = rng.nextInt(minX, maxX + 1);
            coords[i][1] = rng.nextInt(minZ, maxZ + 1);
        }

        tryLocation(world, coords, 0, isNether, isEnd, checkBiomes, minHeight, result);
        return result;
    }

    private void tryLocation(World world, int[][] coords, int idx, boolean isNether,
                             boolean isEnd, boolean checkBiomes, int minHeight,
                             CompletableFuture<Location> result) {
        if (idx >= coords.length) {
            result.complete(null);
            return;
        }

        int x = coords[idx][0];
        int z = coords[idx][1];

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, checkBiomes, false);
            CompletableFuture.runAsync(() -> {
                int localX = x & 0xF;
                int localZ = z & 0xF;
                try {
                    boolean blockedBiome = checkBiomes
                            && blacklistedBiomes.contains(snapshot.getBiome(localX, 64, localZ));
                    if (!blockedBiome) {
                        Location safe = checkSafe(world, snapshot, x, z, localX, localZ,
                                isNether, isEnd, minHeight);
                        if (safe != null) {
                            result.complete(safe);
                            return;
                        }
                    }
                } catch (Exception ignored) {
                }
                tryLocation(world, coords, idx + 1, isNether, isEnd, checkBiomes,
                        minHeight, result);
            });
        }).exceptionally(ex -> {
            tryLocation(world, coords, idx + 1, isNether, isEnd, checkBiomes,
                    minHeight, result);
            return null;
        });
    }

    private Location checkSafe(World world, ChunkSnapshot snapshot, int x, int z,
                               int localX, int localZ, boolean isNether, boolean isEnd,
                               int minHeight) {
        if (isNether) return checkNetherSafe(world, snapshot, x, z, localX, localZ);
        if (isEnd) return checkEndSafe(world, snapshot, x, z, localX, localZ);
        return checkOverworldSafe(world, snapshot, x, z, localX, localZ, minHeight);
    }

    private Location checkOverworldSafe(World world, ChunkSnapshot snapshot, int x, int z,
                                        int localX, int localZ, int minHeight) {
        int topY = snapshot.getHighestBlockYAt(localX, localZ);
        if (topY <= minHeight) return null;

        for (int y = topY; y > minHeight; y--) {
            Material ground = snapshot.getBlockType(localX, y, localZ);
            if (!ground.isSolid() || liquidBlocks.contains(ground)
                    || blacklistedBlocks.contains(ground)) continue;

            Material feet = snapshot.getBlockType(localX, y + 1, localZ);
            if (!isSafe(feet) || blacklistedBlocks.contains(feet)) continue;

            Material head = snapshot.getBlockType(localX, y + 2, localZ);
            if (!isSafe(head) || blacklistedBlocks.contains(head)) continue;

            Material above = snapshot.getBlockType(localX, y + 3, localZ);
            if (liquidBlocks.contains(above) || unsafePassthrough.contains(above)) continue;

            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }
        return null;
    }

    private Location checkNetherSafe(World world, ChunkSnapshot snapshot, int x, int z,
                                     int localX, int localZ) {
        Location loc = scanRangeSnapshot(world, snapshot, x, z, localX, localZ, 80, 32);
        if (loc != null) return loc;

        loc = scanRangeSnapshot(world, snapshot, x, z, localX, localZ, 120, 81);
        if (loc != null) return loc;

        return scanRangeSnapshot(world, snapshot, x, z, localX, localZ, 31, 5);
    }

    private Location checkEndSafe(World world, ChunkSnapshot snapshot, int x, int z,
                                  int localX, int localZ) {
        for (int y = 120; y >= 1; y--) {
            Material below = snapshot.getBlockType(localX, y - 1, localZ);
            if (!below.isSolid() || blacklistedBlocks.contains(below)) continue;

            if (!isSafe(snapshot.getBlockType(localX, y, localZ))) continue;
            if (!isSafe(snapshot.getBlockType(localX, y + 1, localZ))) continue;

            Material above = snapshot.getBlockType(localX, y + 2, localZ);
            if (liquidBlocks.contains(above) || unsafePassthrough.contains(above)) continue;

            if (!hasGroundBelow(snapshot, localX, localZ, y - 1)) continue;

            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return null;
    }

    private Location scanRangeSnapshot(World world, ChunkSnapshot snapshot, int x, int z,
                                       int localX, int localZ, int fromY, int toY) {
        for (int y = fromY; y >= toY; y--) {
            Material below = snapshot.getBlockType(localX, y - 1, localZ);
            if (!below.isSolid() || blacklistedBlocks.contains(below)
                    || liquidBlocks.contains(below)) continue;

            if (!isSafe(snapshot.getBlockType(localX, y, localZ))) continue;
            if (!isSafe(snapshot.getBlockType(localX, y + 1, localZ))) continue;

            Material above = snapshot.getBlockType(localX, y + 2, localZ);
            if (liquidBlocks.contains(above) || unsafePassthrough.contains(above)) continue;

            return new Location(world, x + 0.5, y, z + 0.5);
        }
        return null;
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
