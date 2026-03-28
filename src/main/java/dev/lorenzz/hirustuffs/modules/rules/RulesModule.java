package dev.lorenzz.hirustuffs.modules.rules;

import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.AbstractModule;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RulesModule extends AbstractModule implements Listener {

    private final Set<UUID> accepted = new HashSet<>();
    private File dataFile;
    private YamlConfiguration dataConfig;
    private RulesCommands rulesCommands;

    public RulesModule(Hiru plugin) {
        super(plugin, "rules");
    }

    @Override
    public void onEnable() {
        dataFile = new File(plugin.getDataFolder(), "data/rules-accepted.yml");
        try {
            new File(plugin.getDataFolder(), "data").mkdirs();
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // Load accepted players
        if (dataConfig.contains("accepted")) {
            for (String uuid : dataConfig.getStringList("accepted")) {
                try { accepted.add(UUID.fromString(uuid)); } catch (Exception ignored) {}
            }
        }

        registerListener(this);
        rulesCommands = new RulesCommands(this);
        plugin.getCommandManager().register(rulesCommands);

        logger.info("[Hiru] RulesModule loaded.");
    }

    @Override
    public void onDisable() {
        super.onDisable();
        saveAccepted();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean forceAccept = plugin.getConfigFile().getBoolean("rules.force-accept-on-first-join", true);
        boolean autoOpen = plugin.getConfigFile().getBoolean("rules.auto-open-on-join", true);

        if (forceAccept && !hasAccepted(player.getUniqueId())) {
            Messages.send(player, "&c&lYou must accept the rules! Use &e/rules &c&lto view them.");

            // Auto-open rules menu after a short delay
            if (autoOpen) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && !hasAccepted(player.getUniqueId())) {
                        rulesCommands.openRulesMenu(player);
                    }
                }, 40L); // 2 seconds delay
            }

            // Kick after configurable delay if not accepted
            int kickDelay = plugin.getConfigFile().getInt("rules.kick-delay-seconds", 60);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !hasAccepted(player.getUniqueId())) {
                    String denyMsg = plugin.getConfigFile().getString("rules.deny-message", "&cYou must accept the rules to play!");
                    player.kick(Messages.colorComponent(denyMsg));
                }
            }, kickDelay * 20L);
        }
    }

    public boolean hasAccepted(UUID uuid) {
        return accepted.contains(uuid);
    }

    public void accept(UUID uuid) {
        accepted.add(uuid);
        saveAccepted();
    }

    private void saveAccepted() {
        dataConfig.set("accepted", accepted.stream().map(UUID::toString).toList());
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[Hiru] Failed to save rules data: " + e.getMessage());
        }
    }
}
