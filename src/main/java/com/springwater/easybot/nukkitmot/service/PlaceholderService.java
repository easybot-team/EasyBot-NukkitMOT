package com.springwater.easybot.nukkitmot.service;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PlaceholderService {
    private static final String API_CLASS = "com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI";

    private final Object api;
    private final Method translateMethod;

    private PlaceholderService(Object api, Method translateMethod) {
        this.api = api;
        this.translateMethod = translateMethod;
    }

    public static PlaceholderService detect(EasyBotNukkitMOT plugin) {
        Plugin papiPlugin = plugin.getServer().getPluginManager().getPlugins().values().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase("PlaceholderAPI"))
                .filter(Plugin::isEnabled)
                .findFirst()
                .orElse(null);
        if (papiPlugin == null) {
            return new PlaceholderService(null, null);
        }

        try {
            Class<?> apiClass = Class.forName(API_CLASS, true, papiPlugin.getClass().getClassLoader());
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method translate = apiClass.getMethod("translateString", String.class, Player.class);
            return new PlaceholderService(api, translate);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("检测到 PlaceholderAPI，但无法接入其 API：" + exception.getMessage());
            return new PlaceholderService(null, null);
        }
    }

    public boolean isAvailable() {
        return api != null && translateMethod != null;
    }

    public String translate(String input, Player player) {
        if (!isAvailable()) {
            throw new IllegalStateException("服务器未安装或未启用兼容的 PlaceholderAPI");
        }
        try {
            String normalized = input == null ? "" : input;
            Object result = translateMethod.invoke(api, normalized, player);
            return result == null ? normalized : result.toString();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法访问 PlaceholderAPI", exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw new IllegalStateException("PlaceholderAPI 处理失败：" + (cause == null ? exception.getMessage() : cause.getMessage()), cause);
        }
    }
}
