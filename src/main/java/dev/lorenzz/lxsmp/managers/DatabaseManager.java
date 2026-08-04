package dev.lorenzz.lxsmp.managers;

import dev.lorenzz.lxsmp.LXSMP;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * DatabaseManager — Provides SQLite storage as an alternative to YAML.
 * Supports economy, stats, and shards data.
 *
 * Usage: DatabaseManager.getInstance().getEconomy(uuid) etc.
 * Only active when database.type is SQLITE in config.yml.
 */
public class DatabaseManager {

    private static DatabaseManager INSTANCE;

    private final LXSMP plugin;
    private Connection connection;
    private boolean usingSQLite;

    public DatabaseManager(LXSMP plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public void init() {
        String type = plugin.getConfigFile().getString("database.type", "YAML").toUpperCase();
        usingSQLite = type.equals("SQLITE");

        if (!usingSQLite) {
            plugin.getLogger().info("[Solum] Database type: YAML (file-based storage).");
            return;
        }

        try {
            String dbFile = plugin.getConfigFile().getString("database.sqlite.file", "solum.db");
            File file = new File(plugin.getDataFolder(), dbFile);
            plugin.getDataFolder().mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
            createTables();
            plugin.getLogger().info("[Solum] Database type: SQLite (" + dbFile + ").");
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum] Failed to connect to SQLite: " + e.getMessage());
            usingSQLite = false;
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS economy (
                    uuid TEXT PRIMARY KEY,
                    balance REAL NOT NULL DEFAULT 0.0
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS stats (
                    uuid TEXT NOT NULL,
                    key TEXT NOT NULL,
                    value INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, key)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS shards (
                    uuid TEXT PRIMARY KEY,
                    amount INTEGER NOT NULL DEFAULT 0
                )
            """);
        }
    }

    public boolean isUsingSQLite() {
        return usingSQLite;
    }

    // ---- Economy ----

    public double getBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM economy WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] getBalance error: " + e.getMessage());
        }
        return 0.0;
    }

    public void setBalance(UUID uuid, double balance) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO economy (uuid, balance) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET balance = ?")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.setDouble(3, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] setBalance error: " + e.getMessage());
        }
    }

    public Map<UUID, Double> getAllBalances() {
        Map<UUID, Double> map = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, balance FROM economy")) {
            while (rs.next()) {
                map.put(UUID.fromString(rs.getString("uuid")), rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] getAllBalances error: " + e.getMessage());
        }
        return map;
    }

    // ---- Stats ----

    public int getStat(UUID uuid, String key) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT value FROM stats WHERE uuid = ? AND key = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("value");
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] getStat error: " + e.getMessage());
        }
        return 0;
    }

    public void setStat(UUID uuid, String key, int value) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO stats (uuid, key, value) VALUES (?, ?, ?) ON CONFLICT(uuid, key) DO UPDATE SET value = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.setInt(3, value);
            ps.setInt(4, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] setStat error: " + e.getMessage());
        }
    }

    public Map<String, Integer> getPlayerStats(UUID uuid) {
        Map<String, Integer> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT key, value FROM stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("key"), rs.getInt("value"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] getPlayerStats error: " + e.getMessage());
        }
        return map;
    }

    public void wipeAllStats() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM stats");
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] wipeAllStats error: " + e.getMessage());
        }
    }

    // ---- Shards ----

    public int getShards(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT amount FROM shards WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("amount");
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] getShards error: " + e.getMessage());
        }
        return 0;
    }

    public void setShards(UUID uuid, int amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO shards (uuid, amount) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET amount = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, amount);
            ps.setInt(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] setShards error: " + e.getMessage());
        }
    }

    public Map<UUID, Integer> getAllShards() {
        Map<UUID, Integer> map = new HashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, amount FROM shards")) {
            while (rs.next()) {
                map.put(UUID.fromString(rs.getString("uuid")), rs.getInt("amount"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("[Solum-DB] getAllShards error: " + e.getMessage());
        }
        return map;
    }

    // ---- Lifecycle ----

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("[Solum] SQLite connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().severe("[Solum] Failed to close SQLite: " + e.getMessage());
            }
        }
    }
}
