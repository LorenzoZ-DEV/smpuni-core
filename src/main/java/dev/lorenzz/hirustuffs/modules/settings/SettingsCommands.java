package dev.lorenzz.hirustuffs.modules.settings;

import dev.lorenzz.common.annotations.Command;
import dev.lorenzz.common.annotations.Permission;
import dev.lorenzz.common.annotations.Sender;
import dev.lorenzz.hirustuffs.Hiru;
import dev.lorenzz.hirustuffs.managers.SettingsManager;
import dev.lorenzz.hirustuffs.utils.GUI;
import dev.lorenzz.hirustuffs.utils.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class SettingsCommands {

    private final Hiru plugin = Hiru.getInstance();

    @Command(name = "settings", aliases = {"options"}, description = "Open settings menu", async = false)
    @Permission("hiru.settings")
    public void settings(@Sender Player player) {
        openSettingsMenu(player);
    }

    private void openSettingsMenu(Player player) {
        SettingsManager sm = SettingsManager.getInstance();
        if (sm == null) { Messages.send(player, "&cSettings not available."); return; }

        Map<String, Boolean> settings = sm.getAllSettings(player.getUniqueId());
        GUI.Builder builder = GUI.builder("&#333333&lPlayer Settings", 4).border(Material.GRAY_STAINED_GLASS_PANE);

        int slot = 10;
        for (Map.Entry<String, Boolean> entry : settings.entrySet()) {
            String key = entry.getKey();
            boolean enabled = entry.getValue();
            String displayName = formatKey(key);
            Material icon = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            String status = enabled ? "&#55FF55ENABLED" : "&#FF4444DISABLED";

            builder.item(slot, icon, "&#E0E0E0&l" + displayName,
                    List.of("", "&#8C8C8CStatus: " + status, "", "&#FFD700&lClick to toggle"),
                    p -> {
                        sm.toggleSetting(p.getUniqueId(), key);
                        openSettingsMenu(p);
                    });
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        builder.open(player);
    }

    private String formatKey(String key) {
        String[] parts = key.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
