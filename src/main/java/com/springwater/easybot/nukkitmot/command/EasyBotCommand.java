package com.springwater.easybot.nukkitmot.command;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.packet.BindStatusAccount;
import com.springwater.easybot.bridge.packet.ConfirmBindResultPacket;
import com.springwater.easybot.bridge.packet.GetNewVersionResultPacket;
import com.springwater.easybot.bridge.packet.GetSocialAccountResultPacket;
import com.springwater.easybot.bridge.packet.QueryBindStatusResultPacket;
import com.springwater.easybot.bridge.packet.StartBindResultPacket;
import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EasyBotCommand implements CommandExecutor {
    private final EasyBotNukkitMOT plugin;

    public EasyBotCommand(EasyBotNukkitMOT plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "bind" -> handleBind(sender);
            case "confirm" -> handleConfirm(sender, args);
            case "bindstatus", "bindings" -> handleBindStatus(sender);
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            case "newversion" -> handleNewVersion(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleBind(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行。");
            return;
        }
        if (!sender.hasPermission("easybot.command.bind")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return;
        }
        if (!plugin.getConfig().getBoolean("command.allow_bind", true)) {
            sender.sendMessage("§c当前服务器未开放账号绑定。");
            return;
        }
        if (!requireBridge(sender)) {
            return;
        }

        String playerName = player.getName();
        String bindStartTemplate = plugin.getConfig().getString(
                "message.bind_start",
                "§f[§a!§f] 开始绑定，请在群内输入“绑定 #code”进行绑定！"
        );
        plugin.executeNetwork(() -> {
            try {
                BridgeClient client = requireClient();
                GetSocialAccountResultPacket account = client.getSocialAccount(playerName);
                if (account != null && account.getName() != null && !account.getName().isBlank()) {
                    sendSync(sender, "§f你的账号已在 §a" + value(account.getPlatform()) + " §f被 §a"
                            + value(account.getName()) + " §f(§a" + value(account.getUuid()) + "§f) 绑定。");
                    sendSync(sender, "§c你已经绑定过账号，请先解绑后再重新绑定。");
                    return;
                }

                StartBindResultPacket result = client.startBind(playerName);
                String message = bindStartTemplate
                        .replace("#code", value(result.getCode()))
                        .replace("#time", value(result.getTime()));
                sendSync(sender, EasyBotNukkitMOT.colorize(message));
            } catch (Exception exception) {
                handleAsyncFailure(sender, "message.bind_fail", "绑定失败", exception);
            }
        });
    }

    private void handleConfirm(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行。");
            return;
        }
        if (!sender.hasPermission("easybot.command.confirm")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return;
        }
        if (args.length < 2 || args[1].isBlank()) {
            sender.sendMessage("§e用法：/easybot confirm <验证码>");
            return;
        }
        if (!requireBridge(sender)) {
            return;
        }

        String code = args[1].trim();
        String playerName = player.getName();
        plugin.executeNetwork(() -> {
            try {
                ConfirmBindResultPacket result = requireClient().confirmBind(playerName, code);
                String message = value(result.getMessage());
                if (message.isBlank()) {
                    message = result.isSuccess() ? "跨平台绑定成功。" : "跨平台绑定失败。";
                }
                sendSync(sender, (result.isSuccess() ? "§a" : "§c") + message);
                if (result.isSuccess() && result.getBoundPlatforms() != null && !result.getBoundPlatforms().isBlank()) {
                    sendSync(sender, "§f已绑定平台：§a" + result.getBoundPlatforms());
                }
            } catch (Exception exception) {
                handleAsyncFailure(sender, null, "确认绑定失败", exception);
            }
        });
    }

    private void handleBindStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c该命令只能由玩家执行。");
            return;
        }
        if (!sender.hasPermission("easybot.command.bindstatus")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return;
        }
        if (!requireBridge(sender)) {
            return;
        }

        String playerName = player.getName();
        plugin.executeNetwork(() -> {
            try {
                QueryBindStatusResultPacket result = requireClient().queryBindStatus(playerName);
                if (!result.isBound()) {
                    sendSync(sender, "§e当前游戏账号尚未绑定任何社交平台。");
                    return;
                }
                sendSync(sender, "§a当前已绑定的社交账号：");
                List<BindStatusAccount> accounts = result.getSocialAccounts();
                if (accounts == null || accounts.isEmpty()) {
                    sendSync(sender, "§7（服务端未返回账号详情）");
                    return;
                }
                for (BindStatusAccount account : accounts) {
                    sendSync(sender, "§f- §a" + value(account.getPlatform()) + "§f："
                            + value(account.getName()) + " §7(" + value(account.getUuid()) + ")");
                }
            } catch (Exception exception) {
                handleAsyncFailure(sender, null, "查询绑定状态失败", exception);
            }
        });
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("easybot.command.status")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return;
        }
        sender.sendMessage("§fEasyBot Bridge：" + (plugin.isBridgeReady() ? "§a已就绪" : "§c未就绪"));
        sender.sendMessage("§f连接地址：§7" + plugin.getConfig().getString("service.url"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("easybot.admin")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return;
        }
        try {
            plugin.reloadPlugin();
            sender.sendMessage("§aEasyBot 配置已重载，Bridge 正在重新连接。");
        } catch (Exception exception) {
            sender.sendMessage("§c重载失败：" + message(exception));
            plugin.getLogger().error("重载 EasyBot 配置失败", exception);
        }
    }

    private void handleNewVersion(CommandSender sender) {
        if (!sender.hasPermission("easybot.admin")) {
            sender.sendMessage("§c你没有权限执行这个命令。");
            return;
        }
        if (!plugin.getConfig().getBoolean("service.update_notify", true)) {
            sender.sendMessage("§e配置中已关闭版本查询。");
            return;
        }
        if (!requireBridge(sender)) {
            return;
        }

        plugin.executeNetwork(() -> {
            try {
                GetNewVersionResultPacket result = requireClient().getNewVersion();
                sendSync(sender, "§bEasyBot 主程序最新版本信息：");
                sendSync(sender, "§f版本：§a" + value(result.getVersionName()));
                sendSync(sender, "§f发布时间：§7" + value(result.getPublishTime()));
                sendSync(sender, "§f下载地址：§7" + value(result.getDownloadUrl()));
                if (result.getUpdateLog() != null && !result.getUpdateLog().isBlank()) {
                    sendSync(sender, "§f更新日志：§7" + result.getUpdateLog());
                }
            } catch (Exception exception) {
                handleAsyncFailure(sender, null, "查询版本失败", exception);
            }
        });
    }

    private boolean requireBridge(CommandSender sender) {
        if (!plugin.isBridgeReady()) {
            sender.sendMessage("§cEasyBot Bridge 尚未就绪，请稍后重试。");
            return false;
        }
        return true;
    }

    private BridgeClient requireClient() {
        BridgeClient client = plugin.getBridgeClient();
        if (client == null || !client.isReady()) {
            throw new IllegalStateException("EasyBot Bridge 尚未就绪");
        }
        return client;
    }

    private void handleAsyncFailure(CommandSender sender, String configPath, String fallback, Exception exception) {
        if (exception instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        String detail = message(exception);
        plugin.runSync(() -> {
            String output = configPath == null
                    ? "§c" + fallback + "：" + detail
                    : plugin.getConfig().getString(configPath, "§f[§c!§f] §c绑定失败：#why")
                    .replace("#why", detail);
            sender.sendMessage(EasyBotNukkitMOT.colorize(output));
        });
        plugin.getLogger().error(fallback, exception);
    }

    private void sendSync(CommandSender sender, String message) {
        plugin.runSync(() -> sender.sendMessage(message));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§b===== EasyBot Nukkit-MOT =====");
        sender.sendMessage("§f/easybot bind §7- 绑定社交账号");
        sender.sendMessage("§f/easybot confirm <验证码> §7- 确认跨平台绑定");
        sender.sendMessage("§f/easybot bindstatus §7- 查看绑定状态");
        sender.sendMessage("§f/easybot status §7- 查看 Bridge 状态");
        sender.sendMessage("§f/esay <消息> §7- 同步消息到群聊");
        if (sender.hasPermission("easybot.admin")) {
            sender.sendMessage("§f/easybot reload §7- 重载配置并重连");
            sender.sendMessage("§f/easybot newversion §7- 查询主程序版本");
        }
    }

    private static String value(Object value) {
        return Objects.toString(value, "");
    }

    private static String message(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }
}
