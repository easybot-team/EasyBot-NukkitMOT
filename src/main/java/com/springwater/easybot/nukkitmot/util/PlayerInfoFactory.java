package com.springwater.easybot.nukkitmot.util;

import cn.nukkit.Player;
import com.springwater.easybot.bridge.model.PlayerInfo;
import com.springwater.easybot.bridge.packet.PlayerInfoWithRaw;

public final class PlayerInfoFactory {
    private PlayerInfoFactory() {
    }

    public static PlayerInfoWithRaw packetInfo(Player player) {
        PlayerInfoWithRaw info = new PlayerInfoWithRaw();
        info.setName(player.getName());
        info.setNameRaw(player.getName());
        info.setUuid(player.getUniqueId().toString());
        info.setIp(safeAddress(player));
        return info;
    }

    public static PlayerInfo playerInfo(Player player) {
        PlayerInfo info = new PlayerInfo();
        info.setPlayerName(player.getName());
        info.setPlayerUuid(player.getUniqueId().toString());
        info.setIp(safeAddress(player));
        info.setBedrock(!player.isJavaClient());
        return info;
    }

    private static String safeAddress(Player player) {
        try {
            String address = player.getAddress();
            return address == null || address.isBlank() ? "127.0.0.1" : address;
        } catch (RuntimeException ignored) {
            return "127.0.0.1";
        }
    }

}
