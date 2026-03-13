package dev.lorenzz.hirustuffs.managers;

import dev.lorenzz.hirustuffs.Hiru;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamsManager {

    private static TeamsManager INSTANCE;

    private final Hiru plugin;
    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public enum Role { LEADER, MODERATOR, MEMBER }

    public static class Team {
        private final String name;
        private UUID leader;
        private final Map<UUID, Role> members = new LinkedHashMap<>();
        private boolean friendlyFire = false;
        private Location home;

        public Team(String name, UUID leader) {
            this.name = name;
            this.leader = leader;
            this.members.put(leader, Role.LEADER);
        }

        public String getName() { return name; }
        public UUID getLeader() { return leader; }
        public Map<UUID, Role> getMembers() { return members; }
        public boolean isFriendlyFire() { return friendlyFire; }
        public void setFriendlyFire(boolean ff) { this.friendlyFire = ff; }
        public Location getHome() { return home; }
        public void setHome(Location home) { this.home = home; }

        public Role getRole(UUID uuid) { return members.getOrDefault(uuid, null); }
        public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
        public boolean isLeaderOrMod(UUID uuid) {
            Role r = members.get(uuid);
            return r == Role.LEADER || r == Role.MODERATOR;
        }
    }

    public TeamsManager(Hiru plugin) {
        this.plugin = plugin;
        INSTANCE = this;
    }

    public static TeamsManager getInstance() { return INSTANCE; }

    public void init() {
        dataFile = new File(plugin.getDataFolder(), "data/teams.yml");
        try {
            Files.createDirectories(Path.of(plugin.getDataFolder().getPath(), "data"));
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) { e.printStackTrace(); }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (!dataConfig.contains("teams")) return;
        ConfigurationSection sec = dataConfig.getConfigurationSection("teams");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            ConfigurationSection ts = sec.getConfigurationSection(name);
            if (ts == null) continue;
            UUID leader = UUID.fromString(ts.getString("leader", ""));
            Team team = new Team(name, leader);
            team.setFriendlyFire(ts.getBoolean("friendly-fire", false));
            if (ts.contains("home")) {
                team.setHome(ts.getLocation("home"));
            }
            ConfigurationSection ms = ts.getConfigurationSection("members");
            if (ms != null) {
                for (String uuid : ms.getKeys(false)) {
                    UUID memberId = UUID.fromString(uuid);
                    Role role = Role.valueOf(ms.getString(uuid, "MEMBER"));
                    team.getMembers().put(memberId, role);
                    playerTeams.put(memberId, name.toLowerCase());
                }
            }
            teams.put(name.toLowerCase(), team);
        }
    }

    public void saveAll() {
        dataConfig = new YamlConfiguration();
        for (Map.Entry<String, Team> entry : teams.entrySet()) {
            Team t = entry.getValue();
            String path = "teams." + t.getName();
            dataConfig.set(path + ".leader", t.getLeader().toString());
            dataConfig.set(path + ".friendly-fire", t.isFriendlyFire());
            if (t.getHome() != null) dataConfig.set(path + ".home", t.getHome());
            for (Map.Entry<UUID, Role> m : t.getMembers().entrySet()) {
                dataConfig.set(path + ".members." + m.getKey().toString(), m.getValue().name());
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) {
            plugin.getLogger().severe("[Hiru] Failed to save teams: " + e.getMessage());
        }
    }

    public Team createTeam(String name, UUID leader) {
        if (teams.containsKey(name.toLowerCase())) return null;
        if (playerTeams.containsKey(leader)) return null;
        Team team = new Team(name, leader);
        teams.put(name.toLowerCase(), team);
        playerTeams.put(leader, name.toLowerCase());
        saveAll();
        return team;
    }

    public boolean disbandTeam(String name) {
        Team team = teams.remove(name.toLowerCase());
        if (team == null) return false;
        for (UUID member : team.getMembers().keySet()) {
            playerTeams.remove(member);
        }
        saveAll();
        return true;
    }

    public boolean addMember(String teamName, UUID player) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null) return false;
        int max = plugin.getConfigFile().getInt("teams.max-members", 10);
        if (team.getMembers().size() >= max) return false;
        team.getMembers().put(player, Role.MEMBER);
        playerTeams.put(player, teamName.toLowerCase());
        saveAll();
        return true;
    }

    public boolean removeMember(String teamName, UUID player) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null) return false;
        if (team.getLeader().equals(player)) return false;
        team.getMembers().remove(player);
        playerTeams.remove(player);
        saveAll();
        return true;
    }

    public boolean setRole(String teamName, UUID player, Role role) {
        Team team = teams.get(teamName.toLowerCase());
        if (team == null || !team.isMember(player)) return false;
        team.getMembers().put(player, role);
        saveAll();
        return true;
    }

    public Team getTeam(String name) { return teams.get(name.toLowerCase()); }
    public Team getPlayerTeam(UUID player) {
        String name = playerTeams.get(player);
        return name != null ? teams.get(name) : null;
    }
    public boolean hasTeam(UUID player) { return playerTeams.containsKey(player); }
    public Collection<Team> getAllTeams() { return Collections.unmodifiableCollection(teams.values()); }

    public void shutdown() { saveAll(); }
}
