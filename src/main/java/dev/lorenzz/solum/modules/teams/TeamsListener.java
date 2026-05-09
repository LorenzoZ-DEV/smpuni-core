package dev.lorenzz.solum.modules.teams;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.managers.TeamsManager;
import dev.lorenzz.solum.managers.TeamsManager.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class TeamsListener implements Listener {

    private final Solum plugin;

    public TeamsListener(Solum plugin) {
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
