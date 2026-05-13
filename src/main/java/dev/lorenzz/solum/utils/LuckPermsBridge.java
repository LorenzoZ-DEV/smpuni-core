package dev.lorenzz.solum.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;

public final class LuckPermsBridge {

    private static final boolean AVAILABLE;
    private static final Class<?> LUCKPERMS_CLASS;
    private static final Method GET_USER_MANAGER;
    private static final Method GET_USER;
    private static final Method GET_PRIMARY_GROUP;

    static {
        boolean available = false;
        Class<?> lpClass = null;
        Method userManagerMethod = null;
        Method getUserMethod = null;
        Method primaryGroupMethod = null;
        try {
            lpClass = Class.forName("net.luckperms.api.LuckPerms");
            userManagerMethod = lpClass.getMethod("getUserManager");
            Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
            getUserMethod = userManagerClass.getMethod("getUser", UUID.class);
            Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
            primaryGroupMethod = userClass.getMethod("getPrimaryGroup");
            available = true;
        } catch (Throwable ignored) {
        }
        AVAILABLE = available;
        LUCKPERMS_CLASS = lpClass;
        GET_USER_MANAGER = userManagerMethod;
        GET_USER = getUserMethod;
        GET_PRIMARY_GROUP = primaryGroupMethod;
    }

    private LuckPermsBridge() {}

    public static boolean isAvailable() {
        return AVAILABLE && Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
    }

    public static String getPrimaryGroup(Player player) {
        if (!isAvailable() || player == null) return "default";
        try {
            @SuppressWarnings({"rawtypes", "unchecked"})
            RegisteredServiceProvider rsp = Bukkit.getServicesManager().getRegistration(LUCKPERMS_CLASS);
            if (rsp == null) return "default";
            Object api = rsp.getProvider();
            Object userManager = GET_USER_MANAGER.invoke(api);
            Object user = GET_USER.invoke(userManager, player.getUniqueId());
            if (user == null) return "default";
            Object group = GET_PRIMARY_GROUP.invoke(user);
            return group != null ? group.toString() : "default";
        } catch (Throwable t) {
            return "default";
        }
    }
}

