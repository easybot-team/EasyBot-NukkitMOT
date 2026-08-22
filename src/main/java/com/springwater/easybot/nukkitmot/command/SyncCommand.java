package com.springwater.easybot.nukkitmot.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.ClientProfile;
import com.springwater.easybot.bridge.packet.PlayerInfoWithRaw;
import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;
import com.springwater.easybot.nukkitmot.util.PlayerInfoFactory;

public final class SyncCommand implements CommandExecutor {
    private final EasyBotNukkitMOT plugin;

    public SyncCommand(EasyBotNukkitMOT plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String message = String.join(" ", args).trim();
        if (message.isEmpty()) {
            sender.sendMessage("§e用法：/esay <消息>");
            return true;
        }
        if (!plugin.isBridgeReady()) {
            sender.sendMessage("§cEasyBot Bridge 尚未就绪，请稍后重试。");
            return true;
        }

        if (sender instanceof Player player) {
            double cost = ClientProfile.getSyncMessageMoney();
            if (cost > 0) {
                if (!plugin.getEconomyService().isAvailable()) {
                    sender.sendMessage(EasyBotNukkitMOT.colorize(plugin.getConfig().getString(
                            "message.economy_unavailable",
                            "§c消息同步已启用收费，但 EconomyAPI 不可用。"
                    )));
                    return true;
                }
                try {
                    if (!plugin.getEconomyService().withdraw(player, cost)) {
                        sender.sendMessage(EasyBotNukkitMOT.colorize(plugin.getConfig().getString(
                                "message.not_enough_money",
                                "§c你的余额不足，发送这条消息需要 #money。"
                        ).replace("#money", Double.toString(cost))));
                        return true;
                    }
                } catch (RuntimeException exception) {
                    sender.sendMessage("§cEconomyAPI 扣款失败，请联系服务器管理员。");
                    plugin.getLogger().error("执行消息同步扣款失败", exception);
                    return true;
                }
            }
        }

        PlayerInfoWithRaw info = sender instanceof Player player
                ? PlayerInfoFactory.packetInfo(player)
                : consoleInfo();
        plugin.executeNetwork(() -> {
            BridgeClient client = plugin.getBridgeClient();
            if (client != null) {
                client.syncMessage(info, message, true);
            }
        });

        sender.sendMessage(EasyBotNukkitMOT.colorize(plugin.getConfig().getString(
                "message.sync_success",
                "§f[§a!§f] §f发送成功！"
        )));
        return true;
    }

    private static PlayerInfoWithRaw consoleInfo() {
        PlayerInfoWithRaw info = new PlayerInfoWithRaw();
        info.setName("CONSOLE");
        info.setNameRaw("CONSOLE");
        info.setUuid("00000000-0000-0000-0000-000000000000");
        info.setIp("127.0.0.1");
        return info;
    }
}
