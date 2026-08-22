package com.springwater.easybot.nukkitmot.bridge;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.Plugin;
import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeBehavior;
import com.springwater.easybot.bridge.ClientProfile;
import com.springwater.easybot.bridge.message.AtSegment;
import com.springwater.easybot.bridge.message.Segment;
import com.springwater.easybot.bridge.model.PlayerInfo;
import com.springwater.easybot.bridge.model.PlayerSkin;
import com.springwater.easybot.bridge.model.ServerInfo;
import com.springwater.easybot.bridge.packet.NbtDataTypeEnum;
import com.springwater.easybot.nukkitmot.EasyBotNukkitMOT;
import com.springwater.easybot.nukkitmot.command.CapturingConsoleCommandSender;
import com.springwater.easybot.nukkitmot.service.MainThreadExecutor;
import com.springwater.easybot.nukkitmot.util.MessageRenderer;
import com.springwater.easybot.nukkitmot.util.PlayerInfoFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class NukkitBridgeBehavior implements BridgeBehavior {
    private final EasyBotNukkitMOT plugin;
    private final MainThreadExecutor mainThread;

    public NukkitBridgeBehavior(EasyBotNukkitMOT plugin, MainThreadExecutor mainThread) {
        this.plugin = plugin;
        this.mainThread = mainThread;
    }

    @Override
    public String runCommand(String playerName, String command, boolean enablePapi) {
        if (!ClientProfile.isCommandSupported()) {
            throw new IllegalStateException("当前服务端不支持远程命令");
        }
        return mainThread.call(() -> {
            String finalCommand = command == null ? "" : command.trim();
            if (enablePapi) {
                Player player = blank(playerName) ? null : plugin.getServer().getPlayerExact(playerName);
                finalCommand = plugin.getPlaceholderService().translate(finalCommand, player);
            }
            while (finalCommand.startsWith("/")) {
                finalCommand = finalCommand.substring(1);
            }
            CapturingConsoleCommandSender sender = new CapturingConsoleCommandSender();
            boolean success = plugin.getServer().dispatchCommand(sender, finalCommand);
            String output = sender.result(success);
            if (!success) {
                throw new IllegalStateException(output);
            }
            return output;
        });
    }

    @Override
    public String papiQuery(String playerName, String query) {
        return mainThread.call(() -> {
            Player player = blank(playerName) ? null : plugin.getServer().getPlayerExact(playerName);
            return plugin.getPlaceholderService().translate(query, player);
        });
    }

    @Override
    public ServerInfo getInfo() {
        return mainThread.call(() -> {
            ServerInfo info = new ServerInfo();
            info.setServerName(plugin.getServer().getName());
            info.setServerVersion(plugin.getServer().getVersion() + " / " + plugin.getServer().getNukkitVersion());
            info.setPluginVersion(plugin.getDescription().getVersion());
            info.setCommandSupported(ClientProfile.isCommandSupported());
            info.setPapiSupported(ClientProfile.isPapiSupported());
            info.setHasGeyser(false);
            info.setOnlineMode(plugin.getServer().xboxAuth);
            return info;
        });
    }

    @Override
    public void SyncToChat(String message) {
        String normalized = message == null ? "" : message;
        plugin.runSync(() -> plugin.getServer().broadcastMessage(normalized));
    }

    @Override
    public void BindSuccessBroadcast(String playerName, String accountId, String accountName) {
        String safePlayerName = Objects.toString(playerName, "");
        plugin.runSync(() -> {
            Player player = plugin.getServer().getPlayerExact(safePlayerName);
            if (player != null) {
                String message = plugin.getConfig().getString(
                                "message.bind_success",
                                "§f[§a!§f] 绑定 §a#account §f(§a#name§f) 成功！"
                        )
                        .replace("#player", safePlayerName)
                        .replace("$player", safePlayerName)
                        .replace("#account", Objects.toString(accountId, ""))
                        .replace("$account", Objects.toString(accountId, ""))
                        .replace("#name", Objects.toString(accountName, ""));
                message = message.replace("$name", Objects.toString(accountName, ""));
                player.sendMessage(EasyBotNukkitMOT.colorize(message));
                player.getLevel().addSound(player, Sound.RANDOM_LEVELUP, 1.0f, 1.0f, player);
            }

            if (plugin.getConfig().getBoolean("event.enable_success_event", false)) {
                for (String configuredCommand : plugin.getConfig().getStringList("event.bind_success")) {
                    if (blank(configuredCommand)) {
                        continue;
                    }
                    String command = configuredCommand
                            .replace("$player", safePlayerName)
                            .replace("$account", Objects.toString(accountId, ""))
                            .replace("$name", Objects.toString(accountName, ""));
                    command = EasyBotNukkitMOT.colorize(command).trim();
                    while (command.startsWith("/")) {
                        command = command.substring(1);
                    }
                    if (!command.isBlank()) {
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                    }
                }
            }
        });
    }

    @Override
    public void KickPlayer(String playerName, String kickMessage) {
        plugin.runSync(() -> {
            Player player = plugin.getServer().getPlayerExact(playerName);
            if (player != null) {
                String reason = blank(kickMessage)
                        ? "§c你已在社交平台解绑账号，请重新完成验证。"
                        : kickMessage;
                player.kick(EasyBotNukkitMOT.colorize(reason), false);
            }
        });
    }

    @Override
    public void SyncToChatExtra(List<Segment> segments, String text) {
        String rendered = MessageRenderer.render(segments, text);
        List<AtSegment> atSegments = segments == null
                ? Collections.emptyList()
                : segments.stream().filter(AtSegment.class::isInstance).map(AtSegment.class::cast).toList();

        plugin.runSync(() -> {
            plugin.getServer().broadcastMessage(rendered);
            if (!plugin.getConfig().getBoolean("event.on_at.enable", true)) {
                return;
            }
            for (Player player : plugin.getServer().getOnlinePlayers().values()) {
                if (isMentioned(player, atSegments, text)) {
                    notifyMention(player);
                }
            }
        });
    }

    @Override
    public boolean moduleIsInstalled(String moduleName) {
        return mainThread.call(() -> findPlugin(moduleName) != null);
    }

    @Override
    public boolean moduleIsEnabled(String moduleName) {
        return mainThread.call(() -> {
            Plugin found = findPlugin(moduleName);
            return found != null && found.isEnabled();
        });
    }

    @Override
    public boolean isAuthenticated(String playerName) {
        return mainThread.call(() -> {
            Player player = plugin.getServer().getPlayerExact(playerName);
            if (player == null || !plugin.getServer().xboxAuth) {
                return true;
            }
            return player.getLoginChainData() != null && player.getLoginChainData().isXboxAuthed();
        });
    }

    @Override
    public JsonObject ReadNbtData(String playerUuid, NbtDataTypeEnum dataType) {
        return mainThread.call(() -> {
            if (blank(playerUuid) || dataType == null) {
                return null;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(playerUuid);
            } catch (IllegalArgumentException exception) {
                return null;
            }

            Player onlinePlayer = plugin.getServer().getOnlinePlayers().get(uuid);
            CompoundTag playerData = !plugin.getServer().savePlayerDataByUuid && onlinePlayer != null
                    ? plugin.getServer().getOfflinePlayerData(onlinePlayer.getName(), false)
                    : plugin.getServer().getOfflinePlayerData(uuid, false);
            if (playerData == null) {
                return null;
            }

            Object value;
            if (dataType == NbtDataTypeEnum.PlayerData) {
                value = playerData.parseValue();
            } else if (dataType == NbtDataTypeEnum.Advancements && playerData.containsCompound("Achievements")) {
                value = playerData.getCompound("Achievements").parseValue();
            } else if (dataType == NbtDataTypeEnum.Statistics && playerData.containsCompound("Statistics")) {
                value = playerData.getCompound("Statistics").parseValue();
            } else {
                return null;
            }
            return com.springwater.easybot.bridge.BridgeClient.getGson().toJsonTree(value).getAsJsonObject();
        });
    }

    @Override
    public List<PlayerInfo> getPlayerList() {
        return mainThread.call(() -> plugin.getServer().getOnlinePlayers().values().stream()
                .filter(Player::isOnline)
                .map(PlayerInfoFactory::playerInfo)
                .collect(Collectors.toList()));
    }

    @Override
    public PlayerSkin getPlayerSkin(String playerName) {
        return mainThread.call(() -> {
            Player player = plugin.getServer().getPlayerExact(playerName);
            return player == null ? null : PlayerInfoFactory.playerSkin(player);
        });
    }

    @Override
    public void onCrossBindNotify(String playerName, String code, String targetPlatform, String originPlatform) {
        plugin.runSync(() -> {
            Player player = plugin.getServer().getPlayerExact(playerName);
            if (player == null) {
                return;
            }
            String message = plugin.getConfig().getString(
                            "message.cross_bind",
                            "§e收到跨平台绑定请求：#origin -> #target，输入 /easybot confirm #code 确认。"
                    )
                    .replace("#code", Objects.toString(code, ""))
                    .replace("#target", Objects.toString(targetPlatform, ""))
                    .replace("#origin", Objects.toString(originPlatform, ""));
            player.sendMessage(EasyBotNukkitMOT.colorize(message));
        });
    }

    public static String getKiller(Player player) {
        Entity killer = player.getKiller();
        if (killer != null) {
            return entityName(killer);
        }
        EntityDamageEvent cause = player.getLastDamageCause();
        if (cause instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() != null) {
            return entityName(byEntity.getDamager());
        }
        if (cause instanceof EntityDamageByBlockEvent byBlock && byBlock.getDamager() != null) {
            return byBlock.getDamager().getName();
        }
        return cause == null ? "未知" : cause.getCause().name();
    }

    private static String entityName(Entity entity) {
        return entity.hasCustomName() ? entity.getNameTag() : entity.getName();
    }

    private Plugin findPlugin(String moduleName) {
        if (blank(moduleName)) {
            return null;
        }
        String expected = moduleName.toLowerCase(Locale.ROOT);
        return plugin.getServer().getPluginManager().getPlugins().values().stream()
                .filter(candidate -> candidate.getName().toLowerCase(Locale.ROOT).equals(expected))
                .findFirst()
                .orElse(null);
    }

    private boolean isMentioned(Player player, List<AtSegment> segments, String text) {
        for (AtSegment segment : segments) {
            if ("0".equals(segment.getAtUserId())) {
                return true;
            }
            String[] playerNames = segment.getAtPlayerNames();
            if (playerNames != null && Arrays.stream(playerNames).anyMatch(player.getName()::equalsIgnoreCase)) {
                return true;
            }
        }

        return plugin.getConfig().getBoolean("event.on_at.find", true)
                && text != null
                && text.toLowerCase(Locale.ROOT).contains(player.getName().toLowerCase(Locale.ROOT));
    }

    private void notifyMention(Player player) {
        String title = EasyBotNukkitMOT.colorize(plugin.getConfig().getString("event.on_at.title", "§a有人@你"));
        String subtitle = EasyBotNukkitMOT.colorize(plugin.getConfig().getString("event.on_at.sub_title", "§a请及时处理"));
        player.sendTitle(title, subtitle);

        if (plugin.getConfig().getBoolean("event.on_at.play_sound", true)) {
            Sound sound = switch (plugin.getConfig().getInt("event.on_at.sound", 0)) {
                case 1 -> Sound.NOTE_BASS;
                case 2 -> Sound.CHIME_AMETHYST_BLOCK;
                default -> Sound.RANDOM_ANVIL_LAND;
            };
            player.getLevel().addSound(player, sound, 1.0f, 1.0f, player);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
