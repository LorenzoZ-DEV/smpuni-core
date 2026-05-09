package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private static DuelManager INSTANCE;

    private final Solum plugin;
    private final Map<UUID, UUID> pendingChallenges = new ConcurrentHashMap<>(); // target -> challenger
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();
    private final Map<UUID, DuelState> activeDuels = new ConcurrentHashMap<>(); // player UUID -> state
    private final Map<UUID, BossBar> duelBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> duelTimerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> duelStartTimes = new ConcurrentHashMap<>();

    // Arena locations
    private Location pos1;
    private Location pos2;
    private Location center;
    private Location dieSpawn;

    private File arenaFile;
    private YamlConfiguration arenaConfig;

    public DuelManager(Solum plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    private Sound getSound(String key, Sound fallback) {
        try {
            return Sound.valueOf(plugin.getConfigFile().getString("sounds." + key, fallback.name()));
        } catch (Exception e) {
            return fallback;
        }
    }

    public static DuelManager getInstance() {
        return INSTANCE;
    }

    public static class DuelState {
        private final UUID opponent;
        private final Location previousLocation;
        private final ItemStack[] previousInventory;
        private final ItemStack[] previousArmor;
        private final double previousHealth;
        private final int previousFoodLevel;
        private final GameMode previousGameMode;

        public DuelState(UUID opponent, Player player) {
            this.opponent = opponent;
            this.previousLocation = player.getLocation().clone();
            this.previousInventory = player.getInventory().getContents().clone();
            this.previousArmor = player.getInventory().getArmorContents().clone();
            this.previousHealth = player.getHealth();
            this.previousFoodLevel = player.getFoodLevel();
            this.previousGameMode = player.getGameMode();
        }

        public UUID getOpponent() { return opponent; }
        public Location getPreviousLocation() { return previousLocation; }
        public ItemStack[] getPreviousInventory() { return previousInventory; }
        public ItemStack[] getPreviousArmor() { return previousArmor; }
        public double getPreviousHealth() { return previousHealth; }
        public int getPreviousFoodLevel() { return previousFoodLevel; }
        public GameMode getPreviousGameMode() { return previousGameMode; }
    }

    public void init() {
        arenaFile = new File(plugin.getDataFolder(), "duel-arena.yml");
        if (!arenaFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                arenaFile.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                arenaFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create duel-arena.yml: " + e.getMessage());
            }
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaFile);
        loadArena();
    }

    private void loadArena() {
        pos1 = loadLocation("pos1");
        pos2 = loadLocation("pos2");
        center = loadLocation("center");
        dieSpawn = loadLocation("die-spawn");
    }

    private Location loadLocation(String path) {
        if (!arenaConfig.contains(path + ".world")) return null;
        String worldName = arenaConfig.getString(path + ".world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                arenaConfig.getDouble(path + ".x"),
                arenaConfig.getDouble(path + ".y"),
                arenaConfig.getDouble(path + ".z"),
                (float) arenaConfig.getDouble(path + ".yaw"),
                (float) arenaConfig.getDouble(path + ".pitch"));
    }

    private void saveLocation(String path, Location loc) {
        arenaConfig.set(path + ".world", loc.getWorld().getName());
        arenaConfig.set(path + ".x", loc.getX());
        arenaConfig.set(path + ".y", loc.getY());
        arenaConfig.set(path + ".z", loc.getZ());
        arenaConfig.set(path + ".yaw", loc.getYaw());
        arenaConfig.set(path + ".pitch", loc.getPitch());
        saveArenaFile();
    }

    private void saveArenaFile() {
        try {
            arenaConfig.save(arenaFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save duel-arena.yml: " + e.getMessage());
        }
    }

    // ── Arena setters ──

    public void setPos1(Location loc) {
        pos1 = loc.clone();
        saveLocation("pos1", loc);
    }

    public void setPos2(Location loc) {
        pos2 = loc.clone();
        saveLocation("pos2", loc);
    }

    public void setCenter(Location loc) {
        center = loc.clone();
        saveLocation("center", loc);
    }

    public void setDieSpawn(Location loc) {
        dieSpawn = loc.clone();
        saveLocation("die-spawn", loc);
    }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public Location getCenter() { return center; }
    public Location getDieSpawn() { return dieSpawn; }

    public boolean isArenaConfigured() {
        return pos1 != null && pos2 != null && dieSpawn != null;
    }

    // ── Challenge system ──

    public boolean sendChallenge(Player challenger, Player target) {
        int timeout = plugin.getConfigFile().getInt("duel.challenge-timeout-seconds", 30);

        if (isInDuel(challenger.getUniqueId())) {
            Messages.send(challenger, plugin.getConfigFile().getString("duel.messages.already-in-duel",
                    "&cYou are already in a duel!"));
            return false;
        }
        if (isInDuel(target.getUniqueId())) {
            Messages.send(challenger, plugin.getConfigFile().getString("duel.messages.target-in-duel",
                    "&cThat player is already in a duel!"));
            return false;
        }
        if (!isArenaConfigured()) {
            Messages.send(challenger, plugin.getConfigFile().getString("duel.messages.arena-not-set",
                    "&cThe duel arena has not been configured yet!"));
            return false;
        }

        pendingChallenges.put(target.getUniqueId(), challenger.getUniqueId());

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingChallenges.remove(target.getUniqueId(), challenger.getUniqueId())) {
                String expiredMsg = plugin.getConfigFile().getString("duel.messages.expired",
                        "&7The duel challenge has expired.");
                if (challenger.isOnline()) Messages.send(challenger, expiredMsg);
                if (target.isOnline()) Messages.send(target, expiredMsg);
            }
            expiryTasks.remove(target.getUniqueId());
        }, timeout * 20L);
        expiryTasks.put(target.getUniqueId(), task);

        return true;
    }

    public boolean acceptChallenge(Player target) {
        UUID challengerId = pendingChallenges.remove(target.getUniqueId());
        if (challengerId == null) return false;
        cancelExpiry(target.getUniqueId());

        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger == null || !challenger.isOnline()) {
            Messages.send(target, "&cThe challenger is no longer online!");
            return false;
        }

        startDuel(challenger, target);
        return true;
    }

    public boolean denyChallenge(Player target) {
        UUID challengerId = pendingChallenges.remove(target.getUniqueId());
        if (challengerId == null) return false;
        cancelExpiry(target.getUniqueId());

        Player challenger = Bukkit.getPlayer(challengerId);
        if (challenger != null && challenger.isOnline()) {
            Messages.send(challenger, plugin.getConfigFile().getString("duel.messages.denied-by-target",
                            "&b%player% &cdenied your duel challenge.")
                    .replace("%player%", target.getName()));
        }
        return true;
    }

    public boolean hasPendingChallenge(UUID targetId) {
        return pendingChallenges.containsKey(targetId);
    }

    // ── Duel lifecycle ──

    public void startDuel(Player player1, Player player2) {
        // Save states
        DuelState state1 = new DuelState(player2.getUniqueId(), player1);
        DuelState state2 = new DuelState(player1.getUniqueId(), player2);
        activeDuels.put(player1.getUniqueId(), state1);
        activeDuels.put(player2.getUniqueId(), state2);

        int countdown = plugin.getConfigFile().getInt("duel.countdown-seconds", 5);

        // Teleport to positions
        player1.teleport(pos1);
        player2.teleport(pos2);

        // Heal and set game mode
        player1.setHealth(player1.getMaxHealth());
        player1.setFoodLevel(20);
        player1.setSaturation(20f);
        player2.setHealth(player2.getMaxHealth());
        player2.setFoodLevel(20);
        player2.setSaturation(20f);

        // Clear inventory and give kit book
        player1.getInventory().clear();
        player1.getInventory().setArmorContents(new ItemStack[4]);
        player2.getInventory().clear();
        player2.getInventory().setArmorContents(new ItemStack[4]);

        player1.getInventory().setItem(4, createKitBook());
        player2.getInventory().setItem(4, createKitBook());

        String startingMsg = plugin.getConfigFile().getString("duel.messages.starting",
                "&e⚔ Duel starting in &c{seconds} &eseconds!");

        // Countdown
        new BukkitRunnable() {
            int count = countdown;

            @Override
            public void run() {
                if (!player1.isOnline() || !player2.isOnline()) {
                    cancel();
                    return;
                }

                if (count > 0) {
                    String msg = startingMsg.replace("{seconds}", String.valueOf(count));
                    player1.sendActionBar(Messages.colorComponent(msg));
                    player2.sendActionBar(Messages.colorComponent(msg));
                    player1.playSound(player1.getLocation(), getSound("teleport-countdown", Sound.BLOCK_NOTE_BLOCK_PLING), 1f, 1f);
                    player2.playSound(player2.getLocation(), getSound("teleport-countdown", Sound.BLOCK_NOTE_BLOCK_PLING), 1f, 1f);
                    count--;
                } else {
                    cancel();
                    String fightMsg = plugin.getConfigFile().getString("duel.messages.fight", "&c&l⚔ FIGHT!");
                    player1.sendActionBar(Messages.colorComponent(fightMsg));
                    player2.sendActionBar(Messages.colorComponent(fightMsg));
                    Messages.send(player1, fightMsg);
                    Messages.send(player2, fightMsg);
                    player1.playSound(player1.getLocation(), getSound("duel-start", Sound.ENTITY_ENDER_DRAGON_GROWL), 0.5f, 1.5f);
                    player2.playSound(player2.getLocation(), getSound("duel-start", Sound.ENTITY_ENDER_DRAGON_GROWL), 0.5f, 1.5f);

                    // Start BossBar timer
                    startDuelBossBar(player1, player2);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ── Duel Kit ──

    /** Unique display name used to identify the kit book */
    public static final String KIT_BOOK_NAME = "§d§l⚔ Duel Kit §7(Right-click)";

    /**
     * Creates the enchanted book item that players right-click to receive their duel kit.
     */
    public ItemStack createKitBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(Messages.color(KIT_BOOK_NAME));
        meta.setLore(java.util.List.of(
                Messages.color("&7Right-click to receive your"),
                Messages.color("&7combat kit for the duel!"),
                "",
                Messages.color("&e&lRight-click to use")
        ));
        book.setItemMeta(meta);
        return book;
    }

    /**
     * Gives a full combat kit to the player for the duel.
     */
    public void giveDuelKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Armor: full iron protection II (nerfed)
        Enchantment protection = Enchantment.getByKey(NamespacedKey.minecraft("protection"));
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        if (protection != null) helmet.addEnchantment(protection, 2);
        ItemStack chestplate = new ItemStack(Material.IRON_CHESTPLATE);
        if (protection != null) chestplate.addEnchantment(protection, 2);
        ItemStack leggings = new ItemStack(Material.IRON_LEGGINGS);
        if (protection != null) leggings.addEnchantment(protection, 2);
        ItemStack boots = new ItemStack(Material.IRON_BOOTS);
        if (protection != null) boots.addEnchantment(protection, 2);

        player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(chestplate);
        player.getInventory().setLeggings(leggings);
        player.getInventory().setBoots(boots);

        // Sword: diamond sharpness III
        Enchantment sharpness = Enchantment.getByKey(NamespacedKey.minecraft("sharpness"));
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        if (sharpness != null) sword.addEnchantment(sharpness, 3);
        player.getInventory().setItem(0, sword);

        // Bow: power III
        Enchantment power = Enchantment.getByKey(NamespacedKey.minecraft("power"));
        ItemStack bow = new ItemStack(Material.BOW);
        if (power != null) bow.addEnchantment(power, 3);
        player.getInventory().setItem(1, bow);

        // Golden apples (reduced)
        player.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 8));

        // Arrows
        player.getInventory().setItem(8, new ItemStack(Material.ARROW, 16));

        // Cooked beef
        player.getInventory().setItem(3, new ItemStack(Material.COOKED_BEEF, 16));

        // Ender pearls (reduced)
        player.getInventory().setItem(4, new ItemStack(Material.ENDER_PEARL, 2));

        player.updateInventory();
        Messages.send(player, "&a⚔ You received the duel combat kit!");
    }

    /**
     * Checks if the given item is the duel kit book.
     */
    public boolean isKitBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        return meta.getDisplayName().contains("Duel Kit");
    }

    // ── Duel BossBar Timer ──

    private void startDuelBossBar(Player player1, Player player2) {
        int maxDuration = plugin.getConfigFile().getInt("duel.max-duration-seconds", 300);
        long startTime = System.currentTimeMillis();

        duelStartTimes.put(player1.getUniqueId(), startTime);
        duelStartTimes.put(player2.getUniqueId(), startTime);

        // Create shared BossBar
        Component title = Messages.colorComponent("&#FF4444&l⚔ DUEL &#8C8C8C» &#E0E0E0" +
                formatDuelTime(0) + " &#8C8C8C/ &#E0E0E0" + formatDuelTime(maxDuration));
        BossBar bossBar = BossBar.bossBar(title, 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);

        duelBossBars.put(player1.getUniqueId(), bossBar);
        duelBossBars.put(player2.getUniqueId(), bossBar);

        player1.showBossBar(bossBar);
        player2.showBossBar(bossBar);

        // Update timer every second
        BukkitTask timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isInDuel(player1.getUniqueId()) || !isInDuel(player2.getUniqueId())) {
                    cancel();
                    return;
                }

                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                float progress = Math.max(0f, Math.min(1f, 1f - ((float) elapsed / maxDuration)));

                int overtimeThreshold = plugin.getConfigFile().getInt("duel.overtime-threshold-seconds", 300);
                boolean overtime = elapsed >= overtimeThreshold;

                // Build title with overtime indicator
                String titleStr;
                if (overtime) {
                    double mult = getDamageMultiplier(player1.getUniqueId());
                    String multStr = String.format("%.0f%%", (mult - 1.0) * 100);
                    titleStr = "&#FF4444&l⚔ DUEL &#8C8C8C» &#E0E0E0" +
                            formatDuelTime((int) elapsed) + " &#FF4444&l⚠ OVERTIME &#FF6B6B(+" + multStr + " dmg)";
                } else {
                    titleStr = "&#FF4444&l⚔ DUEL &#8C8C8C» &#E0E0E0" +
                            formatDuelTime((int) elapsed) + " &#8C8C8C/ &#E0E0E0" + formatDuelTime(maxDuration);
                }

                Component updatedTitle = Messages.colorComponent(titleStr);
                bossBar.name(updatedTitle);
                bossBar.progress(progress);

                // Change color based on state
                if (overtime) {
                    bossBar.color(BossBar.Color.RED);

                    // Apply overtime health drain — 1 damage (0.5 hearts) per second
                    long overtimeSecs = elapsed - overtimeThreshold;
                    double drainPerTick = 1.0; // 0.5 hearts per second

                    // Increase drain over time
                    if (overtimeSecs >= 180) drainPerTick = 3.0;
                    else if (overtimeSecs >= 120) drainPerTick = 2.0;
                    else if (overtimeSecs >= 60) drainPerTick = 1.5;

                    applyOvertimeDrain(player1, drainPerTick);
                    applyOvertimeDrain(player2, drainPerTick);

                    // ActionBar warning
                    player1.sendActionBar(Messages.colorComponent("&#FF4444&l⚠ OVERTIME! &#FF6B6BYou are taking extra damage!"));
                    player2.sendActionBar(Messages.colorComponent("&#FF4444&l⚠ OVERTIME! &#FF6B6BYou are taking extra damage!"));
                } else if (progress <= 0.2f) {
                    bossBar.color(BossBar.Color.RED);
                } else if (progress <= 0.5f) {
                    bossBar.color(BossBar.Color.YELLOW);
                } else {
                    bossBar.color(BossBar.Color.GREEN);
                }

                // Auto-end duel if time runs out (draw)
                if (elapsed >= maxDuration) {
                    cancel();
                    Messages.send(player1, "&c⏱ Time's up! The duel ended in a draw.");
                    Messages.send(player2, "&c⏱ Time's up! The duel ended in a draw.");
                    // End as draw — no winner
                    endDuel(player1.getUniqueId(), player2.getUniqueId(), false);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        duelTimerTasks.put(player1.getUniqueId(), timerTask);
        duelTimerTasks.put(player2.getUniqueId(), timerTask);
    }

    /**
     * Applies overtime health drain to a player. If health reaches 0, the duel ends.
     */
    private void applyOvertimeDrain(Player player, double damage) {
        if (player == null || !player.isOnline()) return;
        double newHealth = player.getHealth() - damage;
        if (newHealth <= 0) {
            player.setHealth(0); // Will trigger death event and end duel
        } else {
            player.setHealth(newHealth);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.3f, 1.2f);
        }
    }

    private void removeDuelBossBar(UUID playerId) {
        BossBar bar = duelBossBars.remove(playerId);
        if (bar != null) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.hideBossBar(bar);
            }
        }
        BukkitTask task = duelTimerTasks.remove(playerId);
        if (task != null) task.cancel();
        duelStartTimes.remove(playerId);
    }

    private String formatDuelTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Returns how many seconds the duel has been going for a given player, or -1 if not tracked.
     */
    public long getDuelElapsedSeconds(UUID playerId) {
        Long start = duelStartTimes.get(playerId);
        if (start == null) return -1;
        return (System.currentTimeMillis() - start) / 1000;
    }

    /**
     * Returns the damage multiplier for overtime penalty.
     * After 5 minutes (300s), damage increases progressively:
     *   5:00-6:00 → 1.25x
     *   6:00-7:00 → 1.5x
     *   7:00-8:00 → 2.0x
     *   8:00+     → 3.0x
     * Also removes 0.5 hearts per second from both players after 5m via the BossBar task.
     */
    public double getDamageMultiplier(UUID playerId) {
        long elapsed = getDuelElapsedSeconds(playerId);
        if (elapsed < 0) return 1.0;

        int overtimeThreshold = plugin.getConfigFile().getInt("duel.overtime-threshold-seconds", 300);
        if (elapsed < overtimeThreshold) return 1.0;

        long overtime = elapsed - overtimeThreshold;
        if (overtime < 60) return 1.25;
        if (overtime < 120) return 1.5;
        if (overtime < 180) return 2.0;
        return 3.0;
    }

    /**
     * Whether this player's duel is in overtime (past the penalty threshold).
     */
    public boolean isInOvertime(UUID playerId) {
        long elapsed = getDuelElapsedSeconds(playerId);
        if (elapsed < 0) return false;
        int overtimeThreshold = plugin.getConfigFile().getInt("duel.overtime-threshold-seconds", 300);
        return elapsed >= overtimeThreshold;
    }

    public void endDuel(UUID winnerId, UUID loserId, boolean disconnect) {
        DuelState winnerState = activeDuels.remove(winnerId);
        DuelState loserState = activeDuels.remove(loserId);

        // Remove BossBars
        removeDuelBossBar(winnerId);
        removeDuelBossBar(loserId);

        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);

        // Restore loser
        if (loser != null && loser.isOnline() && loserState != null) {
            Location tp = dieSpawn != null ? dieSpawn : loserState.getPreviousLocation();
            loser.teleport(tp);
            loser.getInventory().clear();
            loser.getInventory().setContents(loserState.getPreviousInventory());
            loser.getInventory().setArmorContents(loserState.getPreviousArmor());
            loser.setHealth(loserState.getPreviousHealth());
            loser.setFoodLevel(loserState.getPreviousFoodLevel());
            loser.setGameMode(loserState.getPreviousGameMode());
            loser.updateInventory();
            Messages.send(loser, plugin.getConfigFile().getString("duel.messages.lost",
                            "&cYou lost the duel against &b%player%&c!")
                    .replace("%player%", winner != null ? winner.getName() : "???"));
        }

        // Restore winner
        if (winner != null && winner.isOnline() && winnerState != null) {
            Location tp = dieSpawn != null ? dieSpawn : winnerState.getPreviousLocation();
            try{
                winner.teleport(tp);
            } catch (Exception ex){
                SpawnManager.getInstance().getSpawn(winner.getName());
            }
            winner.getInventory().clear();
            winner.getInventory().setContents(winnerState.getPreviousInventory());
            winner.getInventory().setArmorContents(winnerState.getPreviousArmor());
            winner.setHealth(winner.getMaxHealth());
            winner.setFoodLevel(winnerState.getPreviousFoodLevel());
            winner.setGameMode(winnerState.getPreviousGameMode());
            winner.updateInventory();
            Messages.send(winner, plugin.getConfigFile().getString("duel.messages.won",
                            "&a&l⚔ You won the duel against &b%player%&a!")
                    .replace("%player%", loser != null ? loser.getName() : "???"));
            winner.playSound(winner.getLocation(), getSound("duel-win", Sound.UI_TOAST_CHALLENGE_COMPLETE), 1f, 1f);
        }

        // Broadcast
        String broadcast = plugin.getConfigFile().getString("duel.messages.broadcast-end",
                        "&e⚔ &b%winner% &edefeated &b%loser% &ein a duel!")
                .replace("%winner%", winner != null ? winner.getName() : "???")
                .replace("%loser%", loser != null ? loser.getName() : "???");
        Bukkit.broadcast(Messages.colorComponent(Messages.getPrefix() + broadcast));
    }

    public boolean isInDuel(UUID playerId) {
        return activeDuels.containsKey(playerId);
    }

    public DuelState getDuelState(UUID playerId) {
        return activeDuels.get(playerId);
    }

    private void cancelExpiry(UUID targetId) {
        BukkitTask task = expiryTasks.remove(targetId);
        if (task != null) task.cancel();
    }

    public void shutdown() {
        // Clean up all BossBars
        for (UUID uuid : new HashSet<>(duelBossBars.keySet())) {
            removeDuelBossBar(uuid);
        }

        // End all active duels gracefully
        Set<UUID> processed = new HashSet<>();
        for (Map.Entry<UUID, DuelState> entry : activeDuels.entrySet()) {
            UUID playerId = entry.getKey();
            if (processed.contains(playerId)) continue;
            DuelState state = entry.getValue();
            processed.add(playerId);
            processed.add(state.getOpponent());

            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.teleport(state.getPreviousLocation());
                player.getInventory().clear();
                player.getInventory().setContents(state.getPreviousInventory());
                player.getInventory().setArmorContents(state.getPreviousArmor());
                player.setHealth(state.getPreviousHealth());
                player.setFoodLevel(state.getPreviousFoodLevel());
                player.updateInventory();
            }

            DuelState opponentState = activeDuels.get(state.getOpponent());
            if (opponentState != null) {
                Player opponent = Bukkit.getPlayer(state.getOpponent());
                if (opponent != null && opponent.isOnline()) {
                    opponent.teleport(opponentState.getPreviousLocation());
                    opponent.getInventory().clear();
                    opponent.getInventory().setContents(opponentState.getPreviousInventory());
                    opponent.getInventory().setArmorContents(opponentState.getPreviousArmor());
                    opponent.setHealth(opponentState.getPreviousHealth());
                    opponent.setFoodLevel(opponentState.getPreviousFoodLevel());
                    opponent.updateInventory();
                }
            }
        }
        activeDuels.clear();
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        pendingChallenges.clear();
    }
}
