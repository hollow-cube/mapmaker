package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.player.PlayerSetting;
import org.jetbrains.annotations.NotNull;

public enum CosmeticType {
    // Note: The order here is relevant to the cosmetic selector GUI.
    // Be careful when changing it.

    HAT("hat"),
    BACKWEAR("backwear"),
    ACCESSORY("accessory"),
    PET("pet"),
    EMOTE("emote"),
    PARTICLE("particle"),
    VICTORY_EFFECT("victory_effect"),
    ;

    public static final CosmeticType[] VALUES = values();

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
