package dev.lorenzz.lxsmp.modules.chat;

import dev.lorenzz.lxsmp.LXSMP;
import dev.lorenzz.lxsmp.utils.Messages;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;

public class ChatFormatListener implements Listener {

    private final LXSMP plugin;
    private final boolean placeholderApiAvailable;
    private LuckPerms luckPerms;

    public ChatFormatListener(LXSMP plugin) {
        this.plugin = plugin;
        this.placeholderApiAvailable = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        try {
            RegisteredServiceProvider<LuckPerms> rsp =
                    Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (rsp != null) this.luckPerms = rsp.getProvider();
        } catch (Throwable ignored) {
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String format = plugin.getConfigFile().getString(
                "chat-format.format",
                "{prefix}&f{player}{suffix} &7» &f{message}");

        String typedMessage = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (sender.hasPermission("solum.chat.placeholders") && placeholderApiAvailable) {
            typedMessage = PlaceholderAPI.setPlaceholders(sender, typedMessage);
        }

        String prefix = "";
        String suffix = "";
        String rank = "default";
        if (luckPerms != null) {
            try {
                User user = luckPerms.getUserManager().getUser(sender.getUniqueId());
                if (user != null) {
                    rank = user.getPrimaryGroup();
                    CachedMetaData meta = user.getCachedData().getMetaData();
                    if (meta.getPrefix() != null) prefix = meta.getPrefix();
                    if (meta.getSuffix() != null) suffix = meta.getSuffix();
                }
            } catch (Throwable ignored) {
            }
        }

        String partial = format
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{rank}", capitalize(rank))
                .replace("{player}", sender.getName())
                .replace("{displayname}", sender.getName());

        if (placeholderApiAvailable) {
            partial = PlaceholderAPI.setPlaceholders(sender, partial);
        }

        int idx = partial.indexOf("{message}");
        boolean canUseColors = sender.hasPermission("solum.chat.color");

        Component rendered;
        if (idx < 0) {
            Component msgComp = canUseColors
                    ? Messages.colorComponent(typedMessage)
                    : Component.text(typedMessage);
            rendered = Messages.colorComponent(partial).append(msgComp);
        } else {
            String before = partial.substring(0, idx);
            String after = partial.substring(idx + "{message}".length());
            Component msgComp = canUseColors
                    ? Messages.colorComponent(typedMessage)
                    : Component.text(typedMessage);
            rendered = Messages.colorComponent(before)
                    .append(msgComp)
                    .append(Messages.colorComponent(after));
        }

        final Component finalRendered = rendered;

        event.renderer((source, sourceDisplayName, message, viewer) -> finalRendered);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}


