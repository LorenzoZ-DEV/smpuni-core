package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.utils.Config;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class EconomyManager implements Economy {

    private static EconomyManager INSTANCE;

    private final Hiru plugin;
    private final Map<UUID, Double> balances = new HashMap<>();
    private double startingBalance;
    private String currencyName;
    private String currencySymbol;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public EconomyManager(Hiru plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static EconomyManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        Config config = plugin.getConfigFile();
        startingBalance = config.getDouble("economy.starting-balance", 1000.0);
        currencyName = config.getString("economy.currency-name", "Coins");
        currencySymbol = config.getString("economy.currency-symbol", "$");

        DatabaseManager db = DatabaseManager.getInstance();
        if (db != null && db.isUsingSQLite()) {
            // Load from SQLite into cache
            balances.putAll(db.getAllBalances());
        } else {
            dataFile = new File(plugin.getDataFolder(), "data/economy.yml");
            try {
                Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
                if (!dataFile.exists()) dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            loadFromYaml();
        }

        // Register Vault
        Bukkit.getServicesManager().register(Economy.class, this, plugin, ServicePriority.Normal);
        plugin.getLogger().info("[Hiru] Economy registered with Vault.");
    }

    private void loadFromYaml() {
        if (dataConfig != null && dataConfig.contains("balances")) {
            for (String key : dataConfig.getConfigurationSection("balances").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double bal = dataConfig.getDouble("balances." + key);
                    balances.put(uuid, bal);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveAll() {
        DatabaseManager db = DatabaseManager.getInstance();
        if (db != null && db.isUsingSQLite()) {
            for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                db.setBalance(entry.getKey(), entry.getValue());
            }
        } else if (dataConfig != null) {
            for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                dataConfig.set("balances." + entry.getKey().toString(), entry.getValue());
            }
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("[Hiru] Failed to save economy data: " + e.getMessage());
            }
        }
    }

    public void shutdown() {
        saveAll();
        Bukkit.getServicesManager().unregister(Economy.class, this);
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, startingBalance);
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, amount);
        saveAll();
    }

    public void addBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public boolean removeBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) return false;
        setBalance(uuid, current - amount);
        return true;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public List<Map.Entry<UUID, Double>> getTopBalances(int limit) {
        return balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ===== Vault Economy Implementation =====

    @Override public boolean isEnabled() { return true; }
    @Override public String getName() { return "Hiru"; }
    @Override public boolean hasBankSupport() { return false; }
    @Override public int fractionalDigits() { return 2; }
    @Override public String format(double amount) { return currencySymbol + String.format("%.2f", amount); }
    @Override public String currencyNamePlural() { return currencyName; }
    @Override public String currencyNameSingular() { return currencyName; }

    @Override
    public boolean hasAccount(OfflinePlayer player) { return true; }

    @Override
    public boolean hasAccount(String playerName) { return true; }

    @Override
    public boolean hasAccount(String playerName, String worldName) { return hasAccount(playerName); }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) { return hasAccount(player); }

    @Override
    public double getBalance(OfflinePlayer player) {
        return getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(playerName);
        return getBalance(p.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) { return getBalance(playerName); }

    @Override
    public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative");
        if (!has(player, amount)) return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        removeBalance(player.getUniqueId(), amount);
        return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative");
        addBalance(player.getUniqueId(), amount);
        return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "No bank support"); }
    @Override public List<String> getBanks() { return Collections.emptyList(); }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        if (!balances.containsKey(player.getUniqueId())) {
            balances.put(player.getUniqueId(), startingBalance);
            saveAll();
        }
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return createPlayerAccount(Bukkit.getOfflinePlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
}
