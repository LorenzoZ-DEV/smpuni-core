package dev.lorenzz.hirustuffs.modules.teams;

import dev.lorenzz.common.annotations.*;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.TeamsManager;
import dev.lorenzz.hirustuffs.managers.TeamsManager.Team;
import dev.lorenzz.hirustuffs.managers.TeamsManager.Role;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamsCommands {

    private final Hiru plugin = Hiru.getInstance();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>(); // invited -> inviter

    @Command(name = "team", description = "Team management", async = false)
    public void teamRoot(@Sender Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        if (tm == null) { Messages.send(player, "&cTeams not available."); return; }

        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            Messages.send(player, "&#8C8C8CYou are not in a team. Use &#FFD700/team create <name>");
            return;
        }
        openTeamGUI(player, team);
    }

    @Subcommand(name = "create", description = "Create a team")
    public void create(@Sender Player player, @Named("name") String name) {
        TeamsManager tm = TeamsManager.getInstance();
        if (tm.hasTeam(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You are already in a team!");
            return;
        }
        int maxLen = plugin.getConfigFile().getInt("teams.name-max-length", 16);
        if (name.length() > maxLen) {
            Messages.send(player, "&#FF4444Team name too long! Max: " + maxLen);
            return;
        }
        Team team = tm.createTeam(name, player.getUniqueId());
        if (team == null) {
            Messages.send(player, "&#FF4444A team with that name already exists!");
            return;
        }
        Messages.send(player, "&#55FF55Team &#FFD700" + name + " &#55FF55created!");
    }

    @Subcommand(name = "disband", description = "Disband your team")
    public void disband(@Sender Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (!team.getLeader().equals(player.getUniqueId())) {
            Messages.send(player, "&#FF4444Only the leader can disband the team!");
            return;
        }
        // Notify members
        for (UUID member : team.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) Messages.send(p, "&#FF4444Team &#FFD700" + team.getName() + " &#FF4444has been disbanded.");
        }
        tm.disbandTeam(team.getName());
    }

    @Subcommand(name = "invite", description = "Invite a player")
    public void invite(@Sender Player player, @Named("player") Player target) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (!team.isLeaderOrMod(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You don't have permission to invite!");
            return;
        }
        if (tm.hasTeam(target.getUniqueId())) {
            Messages.send(player, "&#FF4444That player is already in a team!");
            return;
        }
        pendingInvites.put(target.getUniqueId(), player.getUniqueId());
        Messages.send(player, "&#55FF55Invite sent to &#FFD700" + target.getName());
        Messages.send(target, "&#FFD700" + player.getName() + " &#E0E0E0invited you to team &#FFD700" +
                team.getName() + "&#E0E0E0! Use &#55FF55/team accept &#E0E0E0or &#FF4444/team deny");

        // Expire after 60s
        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingInvites.remove(target.getUniqueId()), 60 * 20L);
    }

    @Subcommand(name = "accept", description = "Accept a team invite")
    public void accept(@Sender Player player) {
        UUID inviterId = pendingInvites.remove(player.getUniqueId());
        if (inviterId == null) { Messages.send(player, "&#FF4444No pending invites."); return; }
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(inviterId);
        if (team == null) { Messages.send(player, "&#FF4444That team no longer exists."); return; }
        if (!tm.addMember(team.getName(), player.getUniqueId())) {
            Messages.send(player, "&#FF4444Team is full!");
            return;
        }
        Messages.send(player, "&#55FF55You joined team &#FFD700" + team.getName() + "&#55FF55!");
        for (UUID member : team.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null && !p.equals(player)) Messages.send(p, "&#FFD700" + player.getName() + " &#55FF55joined the team!");
        }
    }

    @Subcommand(name = "deny", description = "Deny a team invite")
    public void deny(@Sender Player player) {
        UUID inviterId = pendingInvites.remove(player.getUniqueId());
        if (inviterId == null) { Messages.send(player, "&#FF4444No pending invites."); return; }
        Messages.send(player, "&#8C8C8CInvite denied.");
        Player inviter = Bukkit.getPlayer(inviterId);
        if (inviter != null) Messages.send(inviter, "&#FF4444" + player.getName() + " denied your invite.");
    }

    @Subcommand(name = "leave", description = "Leave your team")
    public void leave(@Sender Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (team.getLeader().equals(player.getUniqueId())) {
            Messages.send(player, "&#FF4444Leaders must &#FFD700/team disband &#FF4444or transfer ownership first!");
            return;
        }
        tm.removeMember(team.getName(), player.getUniqueId());
        Messages.send(player, "&#8C8C8CYou left team &#FFD700" + team.getName());
        for (UUID member : team.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) Messages.send(p, "&#FF4444" + player.getName() + " left the team.");
        }
    }

    @Subcommand(name = "kick", description = "Kick a player")
    public void kick(@Sender Player player, @Named("player") Player target) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (!team.isLeaderOrMod(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You don't have permission to kick!");
            return;
        }
        if (!team.isMember(target.getUniqueId())) {
            Messages.send(player, "&#FF4444That player is not in your team!");
            return;
        }
        tm.removeMember(team.getName(), target.getUniqueId());
        Messages.send(player, "&#55FF55Kicked &#FFD700" + target.getName() + " &#55FF55from the team.");
        Messages.send(target, "&#FF4444You have been kicked from team &#FFD700" + team.getName());
    }

    @Subcommand(name = "info", description = "View team info")
    public void info(@Sender Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        openTeamGUI(player, team);
    }

    @Subcommand(name = "toggleff", description = "Toggle friendly fire")
    public void toggleFF(@Sender Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (!team.getLeader().equals(player.getUniqueId())) {
            Messages.send(player, "&#FF4444Only the leader can toggle friendly fire!");
            return;
        }
        team.setFriendlyFire(!team.isFriendlyFire());
        tm.saveAll();
        String status = team.isFriendlyFire() ? "&#55FF55ENABLED" : "&#FF4444DISABLED";
        Messages.send(player, "&#E0E0E0Friendly fire: " + status);
    }

    @Subcommand(name = "sethome", description = "Set team home")
    public void setHome(@Sender Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (!team.isLeaderOrMod(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You don't have permission to set the team home!");
            return;
        }
        team.setHome(player.getLocation().clone());
        tm.saveAll();
        Messages.send(player, "&#55FF55Team home set!");
    }

    @Subcommand(name = "home", description = "Teleport to team home")
    public void home(@Sender Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (team.getHome() == null) { Messages.send(player, "&#FF4444No team home set!"); return; }
        player.teleportAsync(team.getHome());
        Messages.send(player, "&#55FF55Teleported to team home!");
    }

    private void openTeamGUI(Player viewer, Team team) {
        GUI.Builder builder = GUI.builder("&#333333&lTeam: " + team.getName(), 4).border(Material.BLUE_STAINED_GLASS_PANE);

        // Team info item
        String ffStatus = team.isFriendlyFire() ? "&#55FF55Enabled" : "&#FF4444Disabled";
        builder.item(4, Material.SHIELD, "&#FFD700&l" + team.getName(),
                List.of("&#8C8C8CMembers: &#E0E0E0" + team.getMembers().size(),
                        "&#8C8C8CFriendly Fire: " + ffStatus));

        // Members
        int slot = 19;
        for (Map.Entry<UUID, Role> entry : team.getMembers().entrySet()) {
            if (slot >= 34) break;
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            String roleColor = switch (entry.getValue()) {
                case LEADER -> "&#FFD700";
                case MODERATOR -> "&#4FC3F7";
                case MEMBER -> "&#E0E0E0";
            };
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey()));
                meta.displayName(Messages.colorComponent(roleColor + name));
                meta.lore(List.of(Messages.colorComponent("&#8C8C8CRole: " + roleColor + entry.getValue().name())));
                head.setItemMeta(meta);
            }
            builder.item(slot, head);
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        builder.open(viewer);
    }
}
