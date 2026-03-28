package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private static DuelManager INSTANCE;

    private final Hiru plugin;
    private final Map<UUID, UUID> pendingChallenges = new ConcurrentHashMap<>(); // target -> challenger
    private final Map<UUID, BukkitTask> expiryTasks = new ConcurrentHashMap<>();
    private final Map<UUID, DuelState> activeDuels = new ConcurrentHashMap<>(); // player UUID -> state

    // Arena locations
    private Location pos1;
    private Location pos2;
    private Location center;
    private Location dieSpawn;

    private File arenaFile;
    private YamlConfiguration arenaConfig;

    public DuelManager(Hiru plugin) {
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
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void endDuel(UUID winnerId, UUID loserId, boolean disconnect) {
        DuelState winnerState = activeDuels.remove(winnerId);
        DuelState loserState = activeDuels.remove(loserId);

        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);

        // Restore loser
        if (loser != null && loser.isOnline() && loserState != null) {
            Location tp = dieSpawn != null ? dieSpawn : loserState.getPreviousLocation();
            loser.teleport(tp);
            loser.setHealth(loserState.getPreviousHealth());
            loser.setFoodLevel(loserState.getPreviousFoodLevel());
            loser.setGameMode(loserState.getPreviousGameMode());
            Messages.send(loser, plugin.getConfigFile().getString("duel.messages.lost",
                            "&cYou lost the duel against &b%player%&c!")
                    .replace("%player%", winner != null ? winner.getName() : "???"));
        }

        // Restore winner
        if (winner != null && winner.isOnline() && winnerState != null) {
            Location tp = dieSpawn != null ? dieSpawn : winnerState.getPreviousLocation();
            winner.teleport(tp);
            winner.setHealth(winner.getMaxHealth());
            winner.setFoodLevel(winnerState.getPreviousFoodLevel());
            winner.setGameMode(winnerState.getPreviousGameMode());
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
                player.setHealth(state.getPreviousHealth());
                player.setFoodLevel(state.getPreviousFoodLevel());
            }

            DuelState opponentState = activeDuels.get(state.getOpponent());
            if (opponentState != null) {
                Player opponent = Bukkit.getPlayer(state.getOpponent());
                if (opponent != null && opponent.isOnline()) {
                    opponent.teleport(opponentState.getPreviousLocation());
                    opponent.setHealth(opponentState.getPreviousHealth());
                    opponent.setFoodLevel(opponentState.getPreviousFoodLevel());
                }
            }
        }
        activeDuels.clear();
        expiryTasks.values().forEach(BukkitTask::cancel);
        expiryTasks.clear();
        pendingChallenges.clear();
    }
}
