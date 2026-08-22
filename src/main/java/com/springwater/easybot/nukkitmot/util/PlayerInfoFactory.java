package com.springwater.easybot.nukkitmot.util;

import cn.nukkit.Player;
import cn.nukkit.entity.data.Skin;
import com.springwater.easybot.bridge.model.PlayerInfo;
import com.springwater.easybot.bridge.model.PlayerSkin;
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
        info.setSkinUrl(skinUrl(player));
        return info;
    }

    public static PlayerSkin playerSkin(Player player) {
        String skinUrl = skinUrl(player);
        if (skinUrl == null || skinUrl.isBlank()) {
            return null;
        }
        PlayerSkin skin = new PlayerSkin();
        skin.setSkinUrl(skinUrl);
        skin.setCapeUrl(assetUrl(player.getSkin() == null ? null : player.getSkin().getCapeId()));
        return skin;
    }

    private static String safeAddress(Player player) {
        try {
            String address = player.getAddress();
            return address == null || address.isBlank() ? "127.0.0.1" : address;
        } catch (RuntimeException ignored) {
            return "127.0.0.1";
        }
    }

    private static String skinUrl(Player player) {
        Skin skin = player.getSkin();
        if (skin != null) {
            String skinUrl = assetUrl(skin.getSkinId());
            if (skinUrl != null) {
                return skinUrl;
            }
        }
        return player.isJavaClient() ? "https://mc-heads.net/skin/" + player.getUniqueId() : null;
    }

    private static String assetUrl(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.startsWith("https://") || normalized.startsWith("http://") ? normalized : null;
    }
}
