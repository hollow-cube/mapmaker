package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.player.PlayerSetting;
import org.jetbrains.annotations.NotNull;

public enum CosmeticType {
    HEAD("head");

    private final String id;
    private final PlayerSetting<String> setting;

    CosmeticType(String id) {
        this.id = id;
        this.setting = PlayerSetting.String("cosmetic." + id, "");
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull PlayerSetting<String> setting() {
        return setting;
    }
}
