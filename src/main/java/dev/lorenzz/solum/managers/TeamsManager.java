package dev.lorenzz.solum.managers;

import dev.lorenzz.solum.Solum;
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

    private final Solum plugin;
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

    public TeamsManager(Solum plugin) {
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
            // Always make sure the leader is registered in the cache, even if
            // the members section is missing or partial in the data file.
            playerTeams.put(leader, name.toLowerCase());
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
            plugin.getLogger().severe("[Solum] Failed to save teams: " + e.getMessage());
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
        for (Team t : teams.values()) {
            if (!t.getName().equalsIgnoreCase(teamName)) {
                t.getMembers().remove(player);
            }
        }
        playerTeams.remove(player);
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
        if (name == null) return null;
        Team team = teams.get(name);
        // Self-heal: stale mapping (team gone or player not actually a member)
        if (team == null || !team.isMember(player)) {
            playerTeams.remove(player);
            saveAll();
            return null;
        }
        return team;
    }
    public boolean hasTeam(UUID player) { return getPlayerTeam(player) != null; }

    /**
     * Forcefully drops any team membership for the given player. Scans ALL teams
     * (not just the cached mapping) so we can clean up data that drifted out of
     * sync. Returns true if anything was actually changed.
     */
    public boolean forceClear(UUID player) {
        boolean changed = playerTeams.remove(player) != null;
        // Scan every team and yank the uuid out of its members map. If the team
        // had this player as leader, demote+remove gracefully (team becomes
        // headless and gets disbanded on the spot).
        Iterator<Map.Entry<String, Team>> it = teams.entrySet().iterator();
        List<String> toDisband = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, Team> e = it.next();
            Team t = e.getValue();
            if (t.getMembers().remove(player) != null) {
                changed = true;
                if (player.equals(t.getLeader())) {
                    toDisband.add(t.getName());
                }
            }
        }
        for (String name : toDisband) {
            disbandTeam(name);
        }
        if (changed) saveAll();
        return changed;
    }

    /** Returns the names of every team whose members map references {@code player}. */
    public List<String> findTeamsContaining(UUID player) {
        List<String> hits = new ArrayList<>();
        for (Team t : teams.values()) {
            if (t.getMembers().containsKey(player) || player.equals(t.getLeader())) {
                hits.add(t.getName());
            }
        }
        return hits;
    }

    public Collection<Team> getAllTeams() { return Collections.unmodifiableCollection(teams.values()); }

    public void shutdown() { saveAll(); }
}
