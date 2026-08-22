package com.springwater.easybot.nukkitmot.bridge;

import com.springwater.easybot.bridge.logger.ILogger;
import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;

public final class NukkitBridgeLogger implements ILogger {
    private final EasyBotNukkitMOT plugin;

    public NukkitBridgeLogger(EasyBotNukkitMOT plugin) {
        this.plugin = plugin;
    }

    @Override
    public void info(String message) {
        plugin.getLogger().info(sanitize(message));
    }

    @Override
    public void warn(String message) {
        plugin.getLogger().warning(sanitize(message));
    }

    @Override
    public void error(String message) {
        plugin.getLogger().error(sanitize(message));
    }

    @Override
    public void error(String message, Throwable throwable) {
        plugin.getLogger().error(sanitize(message), throwable);
    }

    @Override
    public void debug(String message) {
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().debug(sanitize(message));
        }
    }

    private static String sanitize(String message) {
        if (message != null && message.startsWith("令牌:")) {
            return "令牌: [已隐藏]";
        }
        return message == null ? "" : message;
    }
}
