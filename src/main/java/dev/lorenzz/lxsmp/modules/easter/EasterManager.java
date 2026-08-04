package dev.lorenzz.lxsmp.modules.easter;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class EasterManager {

    private final LXSMP plugin;

    // Data persistence
    private File dataFile;
    private YamlConfiguration dataConfig;

    // Egg locations currently placed in the world
    private final Set<Location> eggLocations = ConcurrentHashMap.newKeySet();

    // Player -> number of eggs collected
    private final Map<UUID, Integer> playerEggs = new ConcurrentHashMap<>();

    // Player names cache
    private final Map<UUID, String> playerNames = new ConcurrentHashMap<>();

    // Original blocks that were replaced by eggs (to restore later)
    private final Map<Location, Material> originalBlocks = new ConcurrentHashMap<>();

    private boolean active = false;
    private BukkitTask spawnTask;
    private BukkitTask dateCheckTask;
    private BukkitTask batchSpawnTask;
    private BukkitTask reminderTask;

    // Config values
    private int totalEggsGoal;
    private int maxEggsInWorld;  // max physical eggs in the world at any time
    private int spawnIntervalTicks;
    private int eggsPerWave;
    private String worldName;

    // Event dates
    private LocalDate startDate;
    private LocalDate endDate;

    // Prizes (commands executed from console)
    private String firstPlaceCommand;
    private String secondPlaceCommand;
    private String thirdPlaceCommand;

    // Win thresholds
    private int firstPlaceEggs;

    // Reminder
    private int reminderIntervalMinutes;

    public EasterManager(LXSMP plugin) {
        this.plugin = plugin;
        loadConfig();
        initDataFile();
        startDateCheckTask();
    }

    private void loadConfig() {
        totalEggsGoal = plugin.getConfigFile().getInt("easter.total-eggs", 300);
        maxEggsInWorld = plugin.getConfigFile().getInt("easter.max-eggs-in-world", 200);
        spawnIntervalTicks = plugin.getConfigFile().getInt("easter.spawn-interval-ticks", 600);
        eggsPerWave = plugin.getConfigFile().getInt("easter.eggs-per-wave", 20);
        worldName = plugin.getConfigFile().getString("easter.world", "world");

        firstPlaceEggs = plugin.getConfigFile().getInt("easter.first-place-eggs", 100);

        // Parse event dates
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try {
            startDate = LocalDate.parse(plugin.getConfigFile().getString("easter.start-date", "02/04/2026"), fmt);
        } catch (DateTimeParseException e) {
            startDate = LocalDate.of(2026, 4, 2);
            plugin.getLogger().warning("[Easter] Invalid start-date in config, using default: 02/04/2026");
        }
        try {
            endDate = LocalDate.parse(plugin.getConfigFile().getString("easter.end-date", "06/04/2026"), fmt);
        } catch (DateTimeParseException e) {
            endDate = LocalDate.of(2026, 4, 6);
            plugin.getLogger().warning("[Easter] Invalid end-date in config, using default: 06/04/2026");
        }

        firstPlaceCommand = plugin.getConfigFile().getString("easter.rewards.first-command", "lp user %player% parent set vip");
        secondPlaceCommand = plugin.getConfigFile().getString("easter.rewards.second-command", "crates give %player% easter 1");
        thirdPlaceCommand = plugin.getConfigFile().getString("easter.rewards.third-command", "kit give %player% vip");

        reminderIntervalMinutes = plugin.getConfigFile().getInt("easter.reminder-interval-minutes", 10);
    }

    /**
     * Initialize the data file for persisting player egg counts
     */
    private void initDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data/easter.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("[Easter] Failed to create data file: " + e.getMessage());
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Load player egg data from easter.yml
     */
    private void loadData() {
        playerEggs.clear();
        playerNames.clear();

        if (dataConfig == null) return;
        ConfigurationSection sec = dataConfig.getConfigurationSection("players");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int eggs = sec.getInt(key + ".eggs", 0);
                String name = sec.getString(key + ".name", "???");
                if (eggs > 0) {
                    playerEggs.put(uuid, eggs);
                    playerNames.put(uuid, name);
                }
            } catch (Exception ignored) {}
        }

        plugin.getLogger().info("[Easter] Loaded " + playerEggs.size() + " player(s) data from easter.yml");
    }

    /**
     * Save player egg data to easter.yml
     */
    public void saveData() {
        if (dataConfig == null || dataFile == null) return;

        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : playerEggs.entrySet()) {
            String path = "players." + entry.getKey().toString();
            dataConfig.set(path + ".eggs", entry.getValue());
            dataConfig.set(path + ".name", playerNames.getOrDefault(entry.getKey(), "???"));
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Easter] Failed to save easter data: " + e.getMessage());
        }
    }

    /**
     * Check if today is within the event date range
     */
    public boolean isWithinEventDates() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }

    /**
     * Periodically check if the event should auto-start or auto-stop based on dates
     */
    private void startDateCheckTask() {
        // Check every 60 seconds (1200 ticks)
        dateCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isWithinEventDates() && !active) {
                plugin.getLogger().info("[Easter] Event auto-starting based on configured dates.");
                start();
            } else if (!isWithinEventDates() && active) {
                plugin.getLogger().info("[Easter] Event auto-ending based on configured dates.");
                endWithPrizes();
            }
        }, 20L, 1200L); // first check after 1 second, then every 60 seconds
    }

    /**
     * Get formatted time remaining until event ends
     */
    public String getTimeRemaining() {
        if (!active) return "Event not active";
        LocalDate today = LocalDate.now();
        if (today.isAfter(endDate)) return "Ended";
        long daysLeft = today.until(endDate).getDays();
        if (daysLeft == 0) return "Last day!";
        return daysLeft + (daysLeft == 1 ? " day" : " days");
    }

    /**
     * Start the Easter egg hunt event
     */
    public void start() {
        if (active) return;
        active = true;

        clearAllEggs();
        loadConfig();

        // Load saved player data (preserves progress across restarts)
        loadData();

        // Broadcast event start
        for (Player p : Bukkit.getOnlinePlayers()) {
            Messages.sendRaw(p, "");
            Messages.sendRaw(p, "&#FFB6C1&l✦ &#FFFACD&lEASTER EGG HUNT &#FFB6C1&l✦");
            Messages.sendRaw(p, "");
            Messages.sendRaw(p, "&#F8E1E8The Easter Egg Hunt has begun!");
            Messages.sendRaw(p, "&#F8E1E8Search and collect the &#D8A7D8&lEaster Eggs &#F8E1E8scattered across &#98D8C8" + worldName + "&#F8E1E8!");
            Messages.sendRaw(p, "");
            Messages.sendRaw(p, "&#FFFACD&l➤ &#F8E1E8Goal: &#A8E6CF" + totalEggsGoal + " eggs &#F8E1E8total to find");
            Messages.sendRaw(p, "&#FFFACD&l➤ &#F8E1E81st Place &#E8E8E8(first to " + firstPlaceEggs + " eggs)&#F8E1E8: &#FFD700VIP");
            Messages.sendRaw(p, "&#FFFACD&l➤ &#F8E1E82nd Place: &#98D8C8Crate Key");
            Messages.sendRaw(p, "&#FFFACD&l➤ &#F8E1E83rd Place: &#D8A7D8VIP Kit");
            Messages.sendRaw(p, "");
            Messages.sendRaw(p, "&#E8E8E8Duration: &#FFFACD" + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " &#E8E8E8→ &#FFFACD" + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            Messages.sendRaw(p, "&#E8E8E8Time remaining: &#FFFACD" + getTimeRemaining());
            Messages.sendRaw(p, "&#E8E8E8Use &#FFFACD/easter top &#E8E8E8to see the leaderboard!");
            Messages.sendRaw(p, "");
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
        }

        // Spawn initial wave of eggs (delayed 5 seconds to let chunks load after server start)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!active) return;
            spawnEggWave(maxEggsInWorld);
        }, 100L);

        // Schedule periodic egg spawns to keep the world populated
        spawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active) return;
            // Don't start a new wave if a batch spawn is still running
            if (batchSpawnTask != null) return;
            // Check if we already reached the total goal
            if (getTotalCollected() >= totalEggsGoal) return;
            int currentEggs = eggLocations.size();
            // Replenish eggs up to maxEggsInWorld
            if (currentEggs < maxEggsInWorld / 2) {
                int toSpawn = Math.min(eggsPerWave, maxEggsInWorld - currentEggs);
                if (toSpawn > 0) {
                    spawnEggWave(toSpawn);
                }
            }
        }, spawnIntervalTicks, spawnIntervalTicks);

        plugin.getLogger().info("[Easter] Event started! Max " + maxEggsInWorld + " eggs in world at a time, goal: " + totalEggsGoal + " total.");

        // Start reminder broadcast
        if (reminderIntervalMinutes > 0) {
            long reminderTicks = reminderIntervalMinutes * 60L * 20L;
            reminderTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!active) return;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Messages.sendRaw(p, "");
                    Messages.sendRaw(p, "&#FFB6C1&l✦ &#FFFACD&lEASTER EGG HUNT &#FFB6C1&l✦");
                    Messages.sendRaw(p, "&#F8E1E8The Easter Egg Hunt is still active! &#D8A7D8&lFind the eggs!");
                    Messages.sendRaw(p, "&#E8E8E8Eggs found: &#A8E6CF" + getTotalCollected() + "&#E8E8E8/&#A8E6CF" + totalEggsGoal + " &#E8E8E8| Remaining: &#FFFACD" + getRemainingEggs());
                    Messages.sendRaw(p, "&#E8E8E8Time remaining: &#FFFACD" + getTimeRemaining() + " &#E8E8E8| Use &#FFFACD/easter top &#E8E8E8for leaderboard!");
                    Messages.sendRaw(p, "");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.5f);
                }
            }, reminderTicks, reminderTicks);
        }
    }

    /**
     * Stop the Easter event and announce winners
     */
    public void stop() {
        if (!active) return;
        active = false;

        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }

        if (batchSpawnTask != null) {
            batchSpawnTask.cancel();
            batchSpawnTask = null;
        }

        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }

        clearAllEggs();

        plugin.getLogger().info("[Easter] Event stopped.");
    }

    /**
     * Fully shut down, including the date check task (called on module disable)
     */
    public void shutdown() {
        saveData();
        stop();
        if (dateCheckTask != null) {
            dateCheckTask.cancel();
            dateCheckTask = null;
        }
    }

    /**
     * End the event and give prizes
     */
    public void endWithPrizes() {
        if (!active) {
            return;
        }

        // Get top 3
        List<Map.Entry<UUID, Integer>> top = getTopPlayers(3);

        // Broadcast results
        for (Player p : Bukkit.getOnlinePlayers()) {
            Messages.sendRaw(p, "");
            Messages.sendRaw(p, "&#FFB6C1&l✦ &#FFFACD&lEASTER EGG HUNT - RESULTS &#FFB6C1&l✦");
            Messages.sendRaw(p, "");

            if (top.size() >= 1) {
                String name = playerNames.getOrDefault(top.get(0).getKey(), "???");
                Messages.sendRaw(p, "&#FFD700&l🥇 1st Place: &#FFFACD" + name + " &#E8E8E8- &#A8E6CF" + top.get(0).getValue() + " eggs &#E8E8E8→ &#FFD700VIP");
            }
            if (top.size() >= 2) {
                String name = playerNames.getOrDefault(top.get(1).getKey(), "???");
                Messages.sendRaw(p, "&#C0C0C0&l🥈 2nd Place: &#FFFACD" + name + " &#E8E8E8- &#A8E6CF" + top.get(1).getValue() + " eggs &#E8E8E8→ &#98D8C8Crate Key");
            }
            if (top.size() >= 3) {
                String name = playerNames.getOrDefault(top.get(2).getKey(), "???");
                Messages.sendRaw(p, "&#CD7F32&l🥉 3rd Place: &#FFFACD" + name + " &#E8E8E8- &#A8E6CF" + top.get(2).getValue() + " eggs &#E8E8E8→ &#D8A7D8VIP Kit");
            }
            Messages.sendRaw(p, "");
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        // Execute reward commands
        if (top.size() >= 1) {
            String name = playerNames.getOrDefault(top.get(0).getKey(), "");
            if (!name.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), firstPlaceCommand.replace("%player%", name));
                plugin.getLogger().info("[Easter] 1st place reward given to " + name);
            }
        }
        if (top.size() >= 2) {
            String name = playerNames.getOrDefault(top.get(1).getKey(), "");
            if (!name.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), secondPlaceCommand.replace("%player%", name));
                plugin.getLogger().info("[Easter] 2nd place reward given to " + name);
            }
        }
        if (top.size() >= 3) {
            String name = playerNames.getOrDefault(top.get(2).getKey(), "");
            if (!name.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), thirdPlaceCommand.replace("%player%", name));
                plugin.getLogger().info("[Easter] 3rd place reward given to " + name);
            }
        }

        stop();
    }

    /**
     * Get all loaded chunks in the easter world (no radius filter).
     */
    private List<Chunk> getLoadedChunksInWorld() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return Collections.emptyList();
        Chunk[] loaded = world.getLoadedChunks();
        List<Chunk> list = new ArrayList<>(loaded.length);
        Collections.addAll(list, loaded);
        return list;
    }

    /**
     * Spawn eggs gradually using a repeating task that places a small batch per tick.
     * Uses ALL loaded chunks in the world — no radius filter, since chunks only exist
     * where players are (or have been). This avoids forcing chunk generation.
     */
    private void spawnEggWave(int count) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[Easter] World '" + worldName + "' not found!");
            return;
        }

        // Cancel any previous batch task
        if (batchSpawnTask != null) {
            batchSpawnTask.cancel();
            batchSpawnTask = null;
        }

        final int eggsPerTick = 5;
        final int attemptsPerTick = 50;
        final AtomicInteger totalSpawned = new AtomicInteger(0);
        final AtomicInteger totalAttempts = new AtomicInteger(0);
        final int maxTotalAttempts = count * 50;
        // Mutable holder for the chunk list so it can be refreshed inside the lambda
        final List<List<Chunk>> chunkHolder = new ArrayList<>();
        chunkHolder.add(getLoadedChunksInWorld());

        batchSpawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!active || totalSpawned.get() >= count || totalAttempts.get() >= maxTotalAttempts) {
                plugin.getLogger().info("[Easter] Batch spawn complete: " + totalSpawned.get() + " eggs placed.");
                if (batchSpawnTask != null) {
                    batchSpawnTask.cancel();
                    batchSpawnTask = null;
                }
                return;
            }

            // Refresh the chunk list every 20 ticks (roughly)
            List<Chunk> currentChunks = chunkHolder.get(0);
            if (currentChunks.isEmpty() || totalAttempts.get() % 100 == 0) {
                currentChunks = getLoadedChunksInWorld();
                chunkHolder.set(0, currentChunks);
            }

            if (currentChunks.isEmpty()) {
                // Still no chunks — skip this tick silently, will retry next tick
                return;
            }

            int spawnedThisTick = 0;
            int attemptsThisTick = 0;

            while (spawnedThisTick < eggsPerTick
                    && attemptsThisTick < attemptsPerTick
                    && totalSpawned.get() < count
                    && totalAttempts.get() < maxTotalAttempts) {

                attemptsThisTick++;
                totalAttempts.incrementAndGet();

                // Pick a random loaded chunk
                Chunk chunk = currentChunks.get(ThreadLocalRandom.current().nextInt(currentChunks.size()));
                if (!chunk.isLoaded()) continue;

                // Random block within the chunk (0-15 offset)
                int x = chunk.getX() * 16 + ThreadLocalRandom.current().nextInt(16);
                int z = chunk.getZ() * 16 + ThreadLocalRandom.current().nextInt(16);

                Block highest = world.getHighestBlockAt(x, z);
                Location eggLoc = highest.getLocation().add(0, 1, 0);
                Block eggBlock = eggLoc.getBlock();

                // Validate placement
                Material eggType = eggBlock.getType();
                Material groundType = highest.getType();
                if ((eggType != Material.AIR && eggType != Material.CAVE_AIR)
                        || groundType == Material.WATER
                        || groundType == Material.LAVA
                        || groundType.name().contains("LEAVES")
                        || groundType == Material.BEDROCK
                        || eggLoc.getBlockY() < world.getMinHeight() + 5) {
                    continue;
                }

                // Skip if there's already an egg at this location
                if (eggLocations.contains(eggLoc)) continue;

                originalBlocks.put(eggLoc, eggType);
                eggBlock.setType(Material.DRAGON_EGG);
                eggLocations.add(eggLoc);
                totalSpawned.incrementAndGet();
                spawnedThisTick++;
            }
        }, 1L, 1L); // run every tick

        plugin.getLogger().info("[Easter] Started batch egg spawning (target: " + count + " eggs).");
    }

    /**
     * Spawn a few eggs in loaded chunks far from a player (used on player join).
     * Eggs are placed between 50 and 250 blocks away so they require exploration.
     * Caps at 5 eggs per join to keep things balanced.
     */
    public void spawnEggsAroundPlayer(Player player) {
        if (!active) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        if (!player.getWorld().getName().equals(worldName)) return;

        // Don't spawn if we already have plenty of eggs or reached the goal
        if (eggLocations.size() >= maxEggsInWorld) return;
        if (getTotalCollected() >= totalEggsGoal) return;

        // Max 5 eggs per player join, never exceed max in world
        int toSpawn = Math.min(5, maxEggsInWorld - eggLocations.size());
        if (toSpawn <= 0) return;

        Location pLoc = player.getLocation();
        int spawned = 0;
        int attempts = 0;
        int maxAttempts = toSpawn * 30;

        final int minDist = 50;   // minimum distance from the player
        final int maxDist = 250;  // maximum distance from the player

        while (spawned < toSpawn && attempts < maxAttempts) {
            attempts++;

            // Random angle + distance between minDist and maxDist
            double angle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            int dist = ThreadLocalRandom.current().nextInt(minDist, maxDist + 1);
            int x = pLoc.getBlockX() + (int) (Math.cos(angle) * dist);
            int z = pLoc.getBlockZ() + (int) (Math.sin(angle) * dist);

            int chunkX = x >> 4;
            int chunkZ = z >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) continue;

            Block highest = world.getHighestBlockAt(x, z);
            Location eggLoc = highest.getLocation().add(0, 1, 0);
            Block eggBlock = eggLoc.getBlock();

            Material eggType = eggBlock.getType();
            Material groundType = highest.getType();
            if ((eggType != Material.AIR && eggType != Material.CAVE_AIR)
                    || groundType == Material.WATER
                    || groundType == Material.LAVA
                    || groundType.name().contains("LEAVES")
                    || groundType == Material.BEDROCK
                    || eggLoc.getBlockY() < world.getMinHeight() + 5) {
                continue;
            }

            if (eggLocations.contains(eggLoc)) continue;

            originalBlocks.put(eggLoc, eggType);
            eggBlock.setType(Material.DRAGON_EGG);
            eggLocations.add(eggLoc);
            spawned++;
        }

        if (spawned > 0) {
            plugin.getLogger().info("[Easter] Spawned " + spawned + " eggs around player " + player.getName() + " (40-200 blocks away).");
        }
    }

    /**
     * Called when a player collects an egg
     */
    public boolean collectEgg(Player player, Location location) {
        if (!active) return false;

        // Normalize location to block coordinates
        Location blockLoc = location.getBlock().getLocation();

        if (!eggLocations.remove(blockLoc)) {
            return false;
        }

        // Restore original block (air)
        Material original = originalBlocks.remove(blockLoc);
        if (original != null) {
            blockLoc.getBlock().setType(original);
        } else {
            blockLoc.getBlock().setType(Material.AIR);
        }

        // Update player count
        UUID uuid = player.getUniqueId();
        playerNames.put(uuid, player.getName());
        int newCount = playerEggs.merge(uuid, 1, Integer::sum);

        // Save data to disk
        saveData();

        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.5f);
        player.spawnParticle(Particle.HEART, location.add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0);

        // Notify player
        Messages.send(player, "&#FFB6C1🥚 &#F8E1E8You found an &#D8A7D8&lEaster Egg&#F8E1E8! &#E8E8E8(" + newCount + " total)");

        // Check if player reached first place threshold
        if (newCount == firstPlaceEggs) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Messages.sendRaw(p, "");
                Messages.sendRaw(p, "&#FFB6C1&l✦ &#FFFACD&l" + player.getName() + " &#F8E1E8reached &#A8E6CF" + firstPlaceEggs + " eggs&#F8E1E8! &#FFB6C1&l✦");
                Messages.sendRaw(p, "");
            }
        }

        // Milestones
        if (newCount % 10 == 0) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                Messages.sendRaw(p, "&#FFB6C1🥚 &#FFFACD" + player.getName() + " &#F8E1E8has found &#A8E6CF" + newCount + " eggs&#F8E1E8!");
            }
        }

        // Check total eggs collected
        int totalCollected = playerEggs.values().stream().mapToInt(Integer::intValue).sum();
        if (totalCollected >= totalEggsGoal) {
            // All eggs found, end event
            Bukkit.getScheduler().runTaskLater(plugin, this::endWithPrizes, 40L);
        }

        return true;
    }

    /**
     * Check if a location is an easter egg
     */
    public boolean isEggLocation(Location location) {
        return eggLocations.contains(location.getBlock().getLocation());
    }

    /**
     * Get top players sorted by eggs collected
     */
    public List<Map.Entry<UUID, Integer>> getTopPlayers(int limit) {
        return playerEggs.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get eggs collected by a player
     */
    public int getPlayerEggs(UUID uuid) {
        return playerEggs.getOrDefault(uuid, 0);
    }

    /**
     * Get total eggs collected by all players
     */
    public int getTotalCollected() {
        return playerEggs.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Get remaining eggs in the world
     */
    public int getRemainingEggs() {
        return eggLocations.size();
    }

    /**
     * Clear all eggs from the world
     */
    private void clearAllEggs() {
        for (Location loc : eggLocations) {
            Material original = originalBlocks.remove(loc);
            Block block = loc.getBlock();
            if (block.getType() == Material.DRAGON_EGG) {
                block.setType(original != null ? original : Material.AIR);
            }
        }
        eggLocations.clear();
        originalBlocks.clear();
    }

    public boolean isActive() {
        return active;
    }

    public int getTotalEggsGoal() {
        return totalEggsGoal;
    }

    public int getFirstPlaceEggs() {
        return firstPlaceEggs;
    }

    public Map<UUID, String> getPlayerNames() {
        return playerNames;
    }
}
