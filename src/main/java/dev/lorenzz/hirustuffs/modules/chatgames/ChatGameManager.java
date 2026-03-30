package dev.lorenzz.hirustuffs.modules.chatgames;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.EconomyManager;
import dev.lorenzz.hirustuffs.managers.ShardManager;
import dev.lorenzz.hirustuffs.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

public class ChatGameManager {

    private final Hiru plugin;
    private BukkitTask gameTask;

    // AtomicReference prevents race conditions between async chat thread and main thread
    private final AtomicReference<ActiveGame> activeGame = new AtomicReference<>(null);

    public enum GameType {
        UNSCRAMBLE,
        MATH,
        TYPE_FIRST,
        TRIVIA
    }

    public ChatGameManager(Hiru plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int intervalSeconds = plugin.getConfigFile().getInt("chatgames.interval-seconds", 300);
        plugin.getLogger().info("[ChatGame] Starting game timer with interval: " + intervalSeconds + "s (" + (intervalSeconds * 20L) + " ticks)");
        // First game after one interval, then repeating
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, this::startNewGame,
                intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void stop() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        activeGame.set(null);
    }

    public ActiveGame getActiveGame() {
        return activeGame.get();
    }

    public void forceStart() {
        plugin.getLogger().info("[ChatGame] Force starting a new game...");
        startNewGame();
    }

    // ── Game Start ──

    private void startNewGame() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        // Expire previous game silently if still active
        ActiveGame old = activeGame.getAndSet(null);
        if (old != null) {
            broadcastExpired(old);
        }

        GameType[] types = GameType.values();
        GameType type = types[ThreadLocalRandom.current().nextInt(types.length)];

        String question;
        String answer;

        switch (type) {
            case UNSCRAMBLE: {
                List<String> words = plugin.getConfigFile().getStringList("chatgames.words");
                if (words.isEmpty()) {
                    words = Arrays.asList("diamond", "emerald", "netherite", "enchantment", "creeper",
                            "enderman", "obsidian", "redstone", "pickaxe", "adventure",
                            "skeleton", "blaze", "fortress", "overworld", "potion");
                }
                answer = words.get(ThreadLocalRandom.current().nextInt(words.size())).toLowerCase();
                String scrambled = scrambleWord(answer);
                question = "Unscramble the word: &b&l" + scrambled.toUpperCase();
                break;
            }
            case MATH: {
                int a, b, result;
                String op;
                int opType = ThreadLocalRandom.current().nextInt(3);
                if (opType == 0) {
                    a = ThreadLocalRandom.current().nextInt(10, 500);
                    b = ThreadLocalRandom.current().nextInt(10, 500);
                    result = a + b;
                    op = "+";
                } else if (opType == 1) {
                    a = ThreadLocalRandom.current().nextInt(50, 500);
                    b = ThreadLocalRandom.current().nextInt(10, a);
                    result = a - b;
                    op = "-";
                } else {
                    a = ThreadLocalRandom.current().nextInt(2, 25);
                    b = ThreadLocalRandom.current().nextInt(2, 25);
                    result = a * b;
                    op = "x";
                }
                question = "Solve: &b&l" + a + " " + op + " " + b;
                answer = String.valueOf(result);
                break;
            }
            case TYPE_FIRST: {
                List<String> phrases = plugin.getConfigFile().getStringList("chatgames.phrases");
                if (phrases.isEmpty()) {
                    phrases = Arrays.asList(
                            "The quick brown fox jumps",
                            "Minecraft is the best game",
                            "UniNet Season One is live",
                            "Diamonds are forever",
                            "Never dig straight down",
                            "The End is near beware",
                            "Creepers gonna creep",
                            "Iron sharpens iron daily"
                    );
                }
                answer = phrases.get(ThreadLocalRandom.current().nextInt(phrases.size()));
                question = "Type first: &b&l" + answer;
                break;
            }
            case TRIVIA: {
                String[][] triviaPool = {
                        {"How many hearts does a player have by default?", "10"},
                        {"What is the maximum enchantment level for Sharpness?", "5"},
                        {"How many obsidian blocks are needed for a nether portal?", "10"},
                        {"What mob drops gunpowder?", "creeper"},
                        {"What ore can only be mined with an iron pickaxe or better?", "diamond"},
                        {"How many slots does a double chest have?", "54"},
                        {"What food gives the most saturation?", "golden carrot"},
                        {"What dimension do Endermen come from?", "the end"},
                        {"How many bookshelves are needed for max enchanting?", "15"},
                        {"What block do you need to respawn in the Nether?", "respawn anchor"},
                };
                int idx = ThreadLocalRandom.current().nextInt(triviaPool.length);
                question = "Trivia: &b&l" + triviaPool[idx][0];
                answer = triviaPool[idx][1].toLowerCase();
                break;
            }
            default: {
                answer = "test";
                question = "Type: &b&ltest";
            }
        }

        int timeoutSeconds = plugin.getConfigFile().getInt("chatgames.timeout-seconds", 30);
        ActiveGame game = new ActiveGame(type, question, answer, System.currentTimeMillis(), timeoutSeconds);
        activeGame.set(game);

        plugin.getLogger().info("[ChatGame] New game started! Type: " + type + " | Answer: '" + answer + "' | Timeout: " + timeoutSeconds + "s");

        // Broadcast the game question
        broadcastQuestion(type, question, timeoutSeconds);

        // Schedule timeout — capture the exact game reference
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Only expire if this exact game is still active (not already answered)
            if (activeGame.compareAndSet(game, null)) {
                broadcastExpired(game);
            }
        }, timeoutSeconds * 20L);
    }

    // ── Answer Handling (called from async chat thread via sync task) ──

    /**
     * Atomically tries to claim the active game for the given player.
     * Uses compareAndSet so only the FIRST correct answer wins — no duplicates.
     * Must be called from the main thread (for reward dispatching).
     */
    public void handleCorrectAnswer(Player player) {
        ActiveGame game = activeGame.get();
        if (game == null || game.isExpired()) {
            plugin.getLogger().info("[ChatGame] handleCorrectAnswer called but game is null or expired for " + player.getName());
            return;
        }

        // Atomic swap: only the first thread to set null wins
        if (activeGame.compareAndSet(game, null)) {
            plugin.getLogger().info("[ChatGame] " + player.getName() + " won! Giving reward...");
            try {
                giveReward(player);
            } catch (Exception e) {
                plugin.getLogger().severe("[ChatGame] Error giving reward to " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                // Still broadcast winner even if reward fails
                broadcastWinner(player, "&cReward Error");
            }
        } else {
            plugin.getLogger().info("[ChatGame] " + player.getName() + " answered correctly but someone was faster!");
        }
    }

    /**
     * Checks if a message matches the current game answer.
     * Safe to call from any thread (read-only, no state mutation).
     */
    public boolean isCorrectAnswer(String message) {
        ActiveGame game = activeGame.get();
        if (game == null || game.isExpired()) return false;
        return message.toLowerCase().trim().equals(game.getAnswer().toLowerCase().trim());
    }

    // ── Rewards ──

    private void giveReward(Player player) {
        List<Map<?, ?>> rewards = plugin.getConfigFile().getMapList("chatgames.rewards");
        plugin.getLogger().info("[ChatGame] Rewards list size: " + rewards.size());
        if (rewards.isEmpty()) {
            plugin.getLogger().info("[ChatGame] No rewards configured, using default");
            giveDefaultReward(player);
            return;
        }

        Map<?, ?> rewardMap = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
        String type = String.valueOf(rewardMap.get("type")).toLowerCase();
        Object amountObj = rewardMap.get("amount");
        int amount = amountObj instanceof Number ? ((Number) amountObj).intValue() : 0;

        String rewardDisplay;

        switch (type) {
            case "money": {
                EconomyManager eco = EconomyManager.getInstance();
                if (eco != null) {
                    eco.addBalance(player.getUniqueId(), amount);
                }
                rewardDisplay = "&a$" + Messages.formatNumber(amount);
                break;
            }
            case "shards": {
                ShardManager shards = ShardManager.getInstance();
                if (shards != null) {
                    shards.addShards(player.getUniqueId(), amount);
                }
                rewardDisplay = "&d" + Messages.formatNumber(amount) + " Shards";
                break;
            }
            case "command": {
                String command = String.valueOf(rewardMap.get("command"));
                Object displayObj = rewardMap.get("display");
                String display = displayObj != null ? String.valueOf(displayObj) : "Special Reward";
                command = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                rewardDisplay = "&b" + display;
                break;
            }
            default: {
                giveDefaultReward(player);
                return;
            }
        }

        broadcastWinner(player, rewardDisplay);
    }

    private void giveDefaultReward(Player player) {
        int roll = ThreadLocalRandom.current().nextInt(3);
        String rewardDisplay;

        switch (roll) {
            case 0: {
                EconomyManager eco = EconomyManager.getInstance();
                if (eco != null) eco.addBalance(player.getUniqueId(), 500);
                rewardDisplay = "&a$500";
                break;
            }
            case 1: {
                ShardManager shards = ShardManager.getInstance();
                if (shards != null) shards.addShards(player.getUniqueId(), 500);
                rewardDisplay = "&d500 Shards";
                break;
            }
            default: {
                String cmd = plugin.getConfigFile().getString("chatgames.default-crate-command",
                        "crate give %player% basic_key 1");
                cmd = cmd.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                rewardDisplay = "&b1x Basic Key";
                break;
            }
        }

        broadcastWinner(player, rewardDisplay);
    }

    // ── Broadcast Methods ──

    private void broadcastQuestion(GameType type, String question, int timeoutSeconds) {
        plugin.getLogger().info("[ChatGame] Broadcasting question to " + Bukkit.getOnlinePlayers().size() + " players");
        Component separator = Messages.colorComponent("&8&m                                                    ");

        Component header = Component.empty()
                .append(Component.text("  \uD83C\uDFAE ", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("CHAT GAME", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" — ", NamedTextColor.GRAY))
                .append(Component.text(getGameTypeName(type), TextColor.color(0x55, 0xFF, 0xFF)).decoration(TextDecoration.BOLD, true));

        Component questionLine = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("» ", TextColor.color(0xFF, 0xAA, 0x00)))
                .append(Messages.colorComponent(question));

        Component timerLine = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("  ⏳ ", TextColor.color(0x4F, 0xC3, 0xF7)))
                .append(Component.text("You have ", NamedTextColor.GRAY))
                .append(Component.text(timeoutSeconds + " seconds", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" to answer in chat!", NamedTextColor.GRAY));

        Component rewardHint = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("  \uD83D\uDCB0 ", TextColor.color(0x55, 0xFF, 0x55)))
                .append(Component.text("Win a random reward: ", NamedTextColor.GRAY))
                .append(Component.text("Coins, Shards, or Crate Key!", TextColor.color(0x55, 0xFF, 0x55)));

        String soundName = plugin.getConfigFile().getString("sounds.chatgame-start", "BLOCK_NOTE_BLOCK_BELL");
        org.bukkit.Sound sound = null;
        try {
            sound = org.bukkit.Sound.valueOf(soundName);
        } catch (IllegalArgumentException ignored) {}

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(Component.empty());
            p.sendMessage(separator);
            p.sendMessage(header);
            p.sendMessage(Component.empty());
            p.sendMessage(questionLine);
            p.sendMessage(Component.empty());
            p.sendMessage(timerLine);
            p.sendMessage(rewardHint);
            p.sendMessage(Component.empty());
            p.sendMessage(separator);
            p.sendMessage(Component.empty());

            if (sound != null) {
                p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
            }
        }
    }

    private void broadcastWinner(Player winner, String rewardDisplay) {
        Component separator = Messages.colorComponent("&8&m                                                    ");

        Component header = Component.empty()
                .append(Component.text("  \uD83C\uDFC6 ", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text("GAME WON!", TextColor.color(0x55, 0xFF, 0x55)).decoration(TextDecoration.BOLD, true));

        Component winnerLine = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("» ", TextColor.color(0xFF, 0xAA, 0x00)))
                .append(Component.text(winner.getName(), TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" answered correctly!", NamedTextColor.GRAY));

        Component rewardLine = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("  \uD83D\uDCB0 ", TextColor.color(0x55, 0xFF, 0x55)))
                .append(Component.text("Reward: ", NamedTextColor.GRAY))
                .append(Messages.colorComponent(rewardDisplay));

        String soundName = plugin.getConfigFile().getString("sounds.chatgame-win", "ENTITY_PLAYER_LEVELUP");
        org.bukkit.Sound sound = null;
        try {
            sound = org.bukkit.Sound.valueOf(soundName);
        } catch (IllegalArgumentException ignored) {}

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(Component.empty());
            p.sendMessage(separator);
            p.sendMessage(header);
            p.sendMessage(winnerLine);
            p.sendMessage(rewardLine);
            p.sendMessage(separator);
            p.sendMessage(Component.empty());

            if (sound != null) {
                p.playSound(p.getLocation(), sound, 1.0f, 1.2f);
            }
        }

        // Title for winner only
        winner.showTitle(Title.title(
                Component.text("\uD83C\uDFC6 YOU WON!", TextColor.color(0xFF, 0xD7, 0x00)).decoration(TextDecoration.BOLD, true),
                Messages.colorComponent(rewardDisplay),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))
        ));
    }

    private void broadcastExpired(ActiveGame game) {
        Component expired = Component.empty()
                .append(Component.text("  \uD83C\uDFAE ", TextColor.color(0xFF, 0x44, 0x44)))
                .append(Component.text("CHAT GAME", TextColor.color(0xFF, 0x44, 0x44)).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" — ", NamedTextColor.GRAY))
                .append(Component.text("Time's up! Nobody answered correctly.", NamedTextColor.GRAY));

        Component answerReveal = Component.text("  ", NamedTextColor.DARK_GRAY)
                .append(Component.text("  ⓘ ", TextColor.color(0x4F, 0xC3, 0xF7)))
                .append(Component.text("The answer was: ", NamedTextColor.GRAY))
                .append(Component.text(game.getAnswer(), TextColor.color(0x55, 0xFF, 0xFF)).decoration(TextDecoration.BOLD, true));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(expired);
            p.sendMessage(answerReveal);
        }
    }

    // ── Helpers ──

    private String scrambleWord(String word) {
        char[] chars = word.toCharArray();
        for (int attempt = 0; attempt < 20; attempt++) {
            for (int i = chars.length - 1; i > 0; i--) {
                int j = ThreadLocalRandom.current().nextInt(i + 1);
                char temp = chars[i];
                chars[i] = chars[j];
                chars[j] = temp;
            }
            if (!new String(chars).equals(word)) break;
        }
        return new String(chars);
    }

    private String getGameTypeName(GameType type) {
        switch (type) {
            case UNSCRAMBLE: return "Unscramble";
            case MATH: return "Math";
            case TYPE_FIRST: return "Type First";
            case TRIVIA: return "Trivia";
            default: return "Game";
        }
    }

    // ── Inner class ──

    public static class ActiveGame {
        private final GameType type;
        private final String question;
        private final String answer;
        private final long startTime;
        private final int timeoutSeconds;

        public ActiveGame(GameType type, String question, String answer, long startTime, int timeoutSeconds) {
            this.type = type;
            this.question = question;
            this.answer = answer;
            this.startTime = startTime;
            this.timeoutSeconds = timeoutSeconds;
        }

        public GameType getType() { return type; }
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public long getStartTime() { return startTime; }

        public boolean isExpired() {
            return (System.currentTimeMillis() - startTime) > (timeoutSeconds * 1000L);
        }

        public long getRemainingSeconds() {
            long remaining = (timeoutSeconds * 1000L) - (System.currentTimeMillis() - startTime);
            return Math.max(0, remaining / 1000L);
        }
    }
}
