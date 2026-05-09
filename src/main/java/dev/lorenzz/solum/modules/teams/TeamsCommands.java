package dev.lorenzz.solum.modules.teams;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.TeamsManager;
import dev.lorenzz.solum.managers.TeamsManager.Team;
import dev.lorenzz.solum.managers.TeamsManager.Role;
import dev.lorenzz.solum.utils.GUI;
import dev.lorenzz.solum.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CommandAlias("team")
@Description("Team management")
public class TeamsCommands extends BaseCommand {

    private final Solum plugin = Solum.getInstance();
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();

    @Default
    public void teamRoot(Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        if (tm == null) { Messages.send(player, "&cTeams not available."); return; }

        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) {
            Messages.send(player, "&#8C8C8CYou are not in a team. Use &#FFD700/team create <name>");
            return;
        }
        openTeamGUI(player, team);
    }

    @Subcommand("create")
    @Description("Create a team")
    public void create(Player player, String name) {
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

    @Subcommand("disband")
    @Description("Disband your team")
    public void disband(Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (!team.getLeader().equals(player.getUniqueId())) {
            Messages.send(player, "&#FF4444Only the leader can disband the team!");
            return;
        }
        for (UUID member : team.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(member);
            if (p != null) Messages.send(p, "&#FF4444Team &#FFD700" + team.getName() + " &#FF4444has been disbanded.");
        }
        tm.disbandTeam(team.getName());
    }

    @Subcommand("invite")
    @Description("Invite a player")
    public void invite(Player player, Player target) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (!team.isLeaderOrMod(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You don't have permission to invite!");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You can't invite yourself!");
            return;
        }
        Team targetTeam = tm.getPlayerTeam(target.getUniqueId());
        if (targetTeam != null) {
            if (targetTeam.getName().equalsIgnoreCase(team.getName())) {
                Messages.send(player, "&#FF4444That player is already in your team!");
            } else {
                Messages.send(player, "&#FF4444" + target.getName() +
                        " &#FF4444is already in another team (&#FFD700" + targetTeam.getName() + "&#FF4444).");
            }
            return;
        }
        if (pendingInvites.containsKey(target.getUniqueId())) {
            Messages.send(player, "&#FF4444That player already has a pending invite!");
            return;
        }
        pendingInvites.put(target.getUniqueId(), player.getUniqueId());
        Messages.send(player, "&#55FF55Invite sent to &#FFD700" + target.getName());
        Messages.send(target, "&#FFD700" + player.getName() + " &#E0E0E0invited you to team &#FFD700" +
                team.getName() + "&#E0E0E0! Use &#55FF55/team accept &#E0E0E0or &#FF4444/team deny");

        Bukkit.getScheduler().runTaskLater(plugin, () -> pendingInvites.remove(target.getUniqueId()), 60 * 20L);
    }

    @Subcommand("accept")
    @Description("Accept a team invite")
    public void accept(Player player) {
        UUID inviterId = pendingInvites.remove(player.getUniqueId());
        if (inviterId == null) { Messages.send(player, "&#FF4444No pending invites."); return; }
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(inviterId);
        if (team == null) { Messages.send(player, "&#FF4444That team no longer exists."); return; }
        Team current = tm.getPlayerTeam(player.getUniqueId());
        if (current != null && current.getLeader().equals(player.getUniqueId())) {
            Messages.send(player, "&#FF4444You are the leader of &#FFD700" + current.getName() +
                    "&#FF4444! Use &#FFD700/team disband &#FF4444first.");
            return;
        }
        tm.forceClear(player.getUniqueId());
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

    @Subcommand("deny")
    @Description("Deny a team invite")
    public void deny(Player player) {
        UUID inviterId = pendingInvites.remove(player.getUniqueId());
        if (inviterId == null) { Messages.send(player, "&#FF4444No pending invites."); return; }
        Messages.send(player, "&#8C8C8CInvite denied.");
        Player inviter = Bukkit.getPlayer(inviterId);
        if (inviter != null) Messages.send(inviter, "&#FF4444" + player.getName() + " denied your invite.");
    }

    @Subcommand("leave")
    @Description("Leave your team")
    public void leave(Player player) {
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

    @Subcommand("kick")
    @Description("Kick a player")
    public void kick(Player player, Player target) {
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

    @Subcommand("forceleave|forcequit")
    @CommandPermission("solum.teams.admin")
    @Description("[Staff] Forcefully clear a player's team membership / stale data")
    public void forceLeave(Player player, String targetName) {
        TeamsManager tm = TeamsManager.getInstance();
        UUID uuid = resolveUuid(targetName);
        if (uuid == null) { Messages.send(player, "&#FF4444Unknown player: " + targetName); return; }
        boolean cleaned = tm.forceClear(uuid);
        Messages.send(player, cleaned
                ? "&#55FF55Cleared team data for &#FFD700" + targetName + " &#8C8C8C(" + uuid + ")"
                : "&#8C8C8CNo team data found for " + targetName + " (" + uuid + ")");
    }

    @Subcommand("whois")
    @CommandPermission("solum.teams.admin")
    @Description("[Staff] Show every team that references this player in data")
    public void whois(Player player, String targetName) {
        TeamsManager tm = TeamsManager.getInstance();
        UUID uuid = resolveUuid(targetName);
        if (uuid == null) { Messages.send(player, "&#FF4444Unknown player: " + targetName); return; }
        List<String> hits = tm.findTeamsContaining(uuid);
        if (hits.isEmpty()) {
            Messages.send(player, "&#55FF55" + targetName + " &#8C8C8C(" + uuid + ") &#E0E0E0is not in any team.");
        } else {
            Messages.send(player, "&#FFD700" + targetName + " &#E0E0E0is referenced in: &#FFD700" + String.join(", ", hits));
            Messages.send(player, "&#8C8C8CUUID: " + uuid + ". Use /team forceleave " + targetName + " to clear.");
        }
    }

    private UUID resolveUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        for (Team t : TeamsManager.getInstance().getAllTeams()) {
            for (UUID member : t.getMembers().keySet()) {
                String cached = Bukkit.getOfflinePlayer(member).getName();
                if (cached != null && cached.equalsIgnoreCase(name)) return member;
            }
        }
        org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(name);
        return off.getUniqueId();
    }

    @Subcommand("info")
    @Description("View team info")
    public void info(Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        openTeamGUI(player, team);
    }

    @Subcommand("toggleff")
    @Description("Toggle friendly fire")
    public void toggleFF(Player player) {
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

    @Subcommand("sethome")
    @Description("Set team home")
    public void setHome(Player player) {
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

    @Subcommand("home")
    @Description("Teleport to team home")
    public void home(Player player) {
        TeamsManager tm = TeamsManager.getInstance();
        Team team = tm.getPlayerTeam(player.getUniqueId());
        if (team == null) { Messages.send(player, "&#FF4444You are not in a team!"); return; }
        if (team.getHome() == null) { Messages.send(player, "&#FF4444No team home set!"); return; }
        player.teleportAsync(team.getHome());
        Messages.send(player, "&#55FF55Teleported to team home!");
    }

    private void openTeamGUI(Player viewer, Team team) {
        GUI.Builder builder = GUI.builder("&#333333&lTeam: " + team.getName(), 4).border(Material.BLUE_STAINED_GLASS_PANE);

        String ffStatus = team.isFriendlyFire() ? "&#55FF55Enabled" : "&#FF4444Disabled";
        builder.item(4, Material.SHIELD, "&#FFD700&l" + team.getName(),
                List.of("&#8C8C8CMembers: &#E0E0E0" + team.getMembers().size(),
                        "&#8C8C8CFriendly Fire: " + ffStatus));

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
