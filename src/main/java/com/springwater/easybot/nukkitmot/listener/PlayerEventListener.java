package com.springwater.easybot.nukkitmot.listener;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.packet.PlayerInfoWithRaw;
import com.springwater.easybot.bridge.packet.PlayerLoginResultPacket;
import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;
import com.springwater.easybot.nukkitmot.bridge.NukkitBridgeBehavior;
import com.springwater.easybot.nukkitmot.util.PlayerInfoFactory;

public final class PlayerEventListener implements Listener {
    private final EasyBotNukkitMOT plugin;

    public PlayerEventListener(EasyBotNukkitMOT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(PlayerAsyncPreLoginEvent event) {
        BridgeClient client = plugin.getBridgeClient();
        try {
            if (client == null || !client.isReady()) {
                throw new IllegalStateException("EasyBot Bridge 尚未就绪");
            }
            String uuid = event.getUuid().toString();
            client.reportPlayer(event.getName(), uuid, event.getAddress());
            PlayerLoginResultPacket result = client.login(event.getName(), uuid);
            if (Boolean.TRUE.equals(result.getKick())) {
                String kickMessage = result.getKickMessage();
                if (kickMessage == null || kickMessage.isBlank()) {
                    kickMessage = "§c请先完成 EasyBot 账号绑定验证。";
                }
                event.disAllow(EasyBotNukkitMOT.colorize(kickMessage));
            }
        } catch (Exception exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().error("处理玩家 " + event.getName() + " 的登录检查失败", exception);
            if (!plugin.isIgnoreBridgeErrors()) {
                event.disAllow("§c服务器账号验证暂时不可用，请稍后重试。");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("skip_options.skip_join", false)) {
            return;
        }
        PlayerInfoWithRaw info = PlayerInfoFactory.packetInfo(event.getPlayer());
        plugin.executeNetwork(() -> {
            BridgeClient client = plugin.getBridgeClient();
            if (client != null) {
                client.syncEnterExit(info, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getConfig().getBoolean("skip_options.skip_quit", false)) {
            return;
        }
        PlayerInfoWithRaw info = PlayerInfoFactory.packetInfo(event.getPlayer());
        plugin.executeNetwork(() -> {
            BridgeClient client = plugin.getBridgeClient();
            if (client != null) {
                client.syncEnterExit(info, false);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(PlayerChatEvent event) {
        if (plugin.getConfig().getBoolean("skip_options.skip_chat", false)) {
            return;
        }
        PlayerInfoWithRaw info = PlayerInfoFactory.packetInfo(event.getPlayer());
        String message = event.getMessage();
        plugin.executeNetwork(() -> {
            BridgeClient client = plugin.getBridgeClient();
            if (client != null) {
                client.syncMessage(info, message, false);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("skip_options.skip_death", false)) {
            return;
        }
        Player player = event.getEntity();
        PlayerInfoWithRaw info = PlayerInfoFactory.packetInfo(player);
        String deathMessage = event.getDeathMessage() == null
                ? ""
                : plugin.getServer().getLanguage().translate(event.getDeathMessage());
        if (deathMessage == null || deathMessage.isBlank()) {
            deathMessage = player.getName() + " 死亡了";
        }
        String killer = NukkitBridgeBehavior.getKiller(player);
        String finalDeathMessage = deathMessage;
        plugin.executeNetwork(() -> {
            BridgeClient client = plugin.getBridgeClient();
            if (client != null) {
                client.syncDeathMessage(info, finalDeathMessage, killer);
            }
        });
    }
}
