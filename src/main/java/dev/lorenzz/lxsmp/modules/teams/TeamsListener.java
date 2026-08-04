package dev.lorenzz.lxsmp.modules.teams;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.managers.TeamsManager;
import dev.lorenzz.lxsmp.managers.TeamsManager.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TeamsListener implements Listener {

    private final LXSMP plugin;

    public TeamsListener(LXSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        TeamsManager tm = TeamsManager.getInstance();
        if (tm == null) return;

        Team attackerTeam = tm.getPlayerTeam(attacker.getUniqueId());
        if (attackerTeam == null) return;
        if (!attackerTeam.isMember(victim.getUniqueId())) return;

        // Same team — check friendly fire
        if (!attackerTeam.isFriendlyFire()) {
            event.setCancelled(true);
        }
    }
}
