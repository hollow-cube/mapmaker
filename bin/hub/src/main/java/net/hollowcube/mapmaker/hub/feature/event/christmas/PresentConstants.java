package net.hollowcube.mapmaker.hub.feature.event.christmas;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.mapmaker.cosmetic.*;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.Nullable;

public class PresentConstants {

    public static final BadSprite WHITE_RED_TEXTURE = BadSprite.require("present_white_red");
    public static final BadSprite WHITE_GOLD_TEXTURE = BadSprite.require("present_white_gold");
    public static final BadSprite RED_GOLD_TEXTURE = BadSprite.require("present_red_gold");
    public static final BadSprite GOLD_GREEN_TEXTURE = BadSprite.require("present_gold_green");
    public static final BadSprite RED_GOLD_GREEN_TEXTURE = BadSprite.require("present_red_gold_green");
    public static final BadSprite RED_GREEN_TEXTURE = BadSprite.require("present_red_green");
    public static final BadSprite GOLD_RED_TEXTURE = BadSprite.require("present_gold_red");

    public static final BadSprite[] TEXTURES = {
            WHITE_RED_TEXTURE,
            WHITE_GOLD_TEXTURE,
            RED_GOLD_TEXTURE,
            GOLD_GREEN_TEXTURE,
            RED_GOLD_GREEN_TEXTURE,
            RED_GREEN_TEXTURE,
            GOLD_RED_TEXTURE
    };

    private final static Int2ObjectMap<Cosmetic> REWARDS = new Int2ObjectArrayMap<>(
            new int[]{1, 5, 10, 15, 20, 25},
            new Cosmetic[]{Hats.ELF_HAT, Accessories.CANDY_CANE, VictoryEffects.JOLLY, Hats.CAKE_HAT, Particles.SNOWFALL, Hats.SANTA_HAT}
    );

    public static BadSprite getTextureForDay(int day) {
        if (day <= 0) return RED_GOLD_GREEN_TEXTURE;
        return TEXTURES[(day - 1) % TEXTURES.length];
    }

    public static @Nullable Cosmetic getRewardForDay(int day) {
        return REWARDS.get(day);
    }

    public static void init() {
        // Dummy init method to force class loading
    }
}
