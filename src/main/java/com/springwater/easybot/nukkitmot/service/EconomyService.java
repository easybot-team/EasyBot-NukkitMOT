package com.springwater.easybot.nukkitmot.service;

import cn.nukkit.Player;
import cn.nukkit.plugin.Plugin;
import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;

import java.lang.reflect.Method;

public final class EconomyService {
    private static final String API_CLASS = "me.onebone.economyapi.EconomyAPI";
    private static final int RET_SUCCESS = 1;

    private final Object api;
    private final Method balanceMethod;
    private final Method withdrawMethod;

    private EconomyService(Object api, Method balanceMethod, Method withdrawMethod) {
        this.api = api;
        this.balanceMethod = balanceMethod;
        this.withdrawMethod = withdrawMethod;
    }

    public static EconomyService detect(EasyBotNukkitMOT plugin) {
        Plugin economyPlugin = plugin.getServer().getPluginManager().getPlugins().values().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase("EconomyAPI"))
                .filter(Plugin::isEnabled)
                .findFirst()
                .orElse(null);
        if (economyPlugin == null) {
            return unavailable();
        }

        try {
            Class<?> apiClass = Class.forName(API_CLASS, true, economyPlugin.getClass().getClassLoader());
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method balance = apiClass.getMethod("myMoney", Player.class);
            Method withdraw = apiClass.getMethod("reduceMoney", Player.class, double.class);
            return new EconomyService(api, balance, withdraw);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("检测到 EconomyAPI，但无法接入其 API：" + exception.getMessage());
            return unavailable();
        }
    }

    private static EconomyService unavailable() {
        return new EconomyService(null, null, null);
    }

    public boolean isAvailable() {
        return api != null && balanceMethod != null && withdrawMethod != null;
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0) {
            return true;
        }
        if (!isAvailable()) {
            throw new IllegalStateException("EconomyAPI 不可用");
        }
        try {
            double balance = ((Number) balanceMethod.invoke(api, player)).doubleValue();
            if (balance < amount) {
                return false;
            }
            int result = ((Number) withdrawMethod.invoke(api, player, amount)).intValue();
            return result == RET_SUCCESS;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("EconomyAPI 扣款失败", exception);
        }
    }
}
