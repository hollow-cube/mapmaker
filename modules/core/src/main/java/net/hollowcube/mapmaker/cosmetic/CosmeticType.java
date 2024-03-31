package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.player.PlayerSetting;
import org.jetbrains.annotations.NotNull;

public enum CosmeticType {
    // Note: The order here is relevant to the cosmetic selector GUI.
    // Be careful when changing it.

    HAT("hat", true),
    BACKWEAR("backwear", false),
    ACCESSORY("accessory", true),
    PET("pet", false),
    EMOTE("emote", false),
    PARTICLE("particle", false),
    VICTORY_EFFECT("victory_effect", false),
    ;

    public static final CosmeticType[] VALUES = values();

    private final String id;
    private final boolean hasModel;
    private final PlayerSetting<String> setting;

    CosmeticType(String id, boolean hasModel) {
        this.id = id;
        this.hasModel = hasModel;
        this.setting = PlayerSetting.String("cosmetic." + id, "");
    }

    public @NotNull String id() {
        return id;
    }

    public boolean hasModel() {
        return hasModel;
    }

    public @NotNull PlayerSetting<String> setting() {
        return setting;
    }
}
