package dev.lorenzz.solum.modules.joinquit;

import dev.lorenzz.solum.Solum;
import dev.lorenzz.solum.utils.Messages;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;

public class JoinQuitListener implements Listener {

    private final Solum plugin;
    private LuckPerms luckPerms;

    public JoinQuitListener(Solum plugin) {
        this.plugin = plugin;
        try {
            RegisteredServiceProvider<LuckPerms> rsp =
                    Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (rsp != null) this.luckPerms = rsp.getProvider();
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            String firstJoinFormat = plugin.getConfigFile().getString(
                    "join-quit.first-join",
                    "&6&l✦ &e{player} &fjoined for the first time! &6&lWELCOME TO SMPUNI!"
            );
            event.joinMessage(format(firstJoinFormat, player));

            List<String> extra = plugin.getConfigFile().getStringList("join-quit.first-join-broadcast");
            if (!extra.isEmpty()) {
                Bukkit.getOnlinePlayers().forEach(p -> {
                    for (String line : extra) {
                        p.sendMessage(format(line, player));
                    }
                });
            }
        } else {
            String joinFormat = plugin.getConfigFile().getString(
                    "join-quit.join",
                    "&8[&a+&8] &7{rank} &f{player} &7has joined the server"
            );
            event.joinMessage(format(joinFormat, player));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String quitFormat = plugin.getConfigFile().getString(
                "join-quit.quit",
                "&8[&c-&8] &7{rank} &f{player} &7has left the server"
        );
        event.quitMessage(format(quitFormat, player));
    }

    private Component format(String raw, Player player) {
        String rank = "";
        String prefix = "";
        String suffix = "";

        if (luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    rank = user.getPrimaryGroup();
                    CachedMetaData meta = user.getCachedData().getMetaData();
                    if (meta.getPrefix() != null) prefix = meta.getPrefix();
                    if (meta.getSuffix() != null) suffix = meta.getSuffix();
                }
            } catch (Throwable ignored) {
            }
        }
        if (rank == null || rank.isEmpty()) rank = "default";

        String out = raw
                .replace("{player}", player.getName())
                .replace("{displayname}", player.getName())
                .replace("{rank}", capitalize(rank))
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix);

        return Messages.colorComponent(out);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}

