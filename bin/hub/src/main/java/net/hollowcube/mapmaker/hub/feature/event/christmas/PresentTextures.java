package net.hollowcube.mapmaker.hub.feature.event.christmas;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;

public class PresentTextures {

    public static final BadSprite WHITE_RED = BadSprite.require("present_white_red");
    public static final BadSprite WHITE_GOLD = BadSprite.require("present_white_gold");
    public static final BadSprite RED_GOLD = BadSprite.require("present_red_gold");
    public static final BadSprite GOLD_GREEN = BadSprite.require("present_gold_green");
    public static final BadSprite RED_GOLD_GREEN = BadSprite.require("present_red_gold_green");
    public static final BadSprite RED_GREEN = BadSprite.require("present_red_green");
    public static final BadSprite GOLD_RED = BadSprite.require("present_gold_red");

    public static final BadSprite[] ALL = {
            WHITE_RED,
            WHITE_GOLD,
            RED_GOLD,
            GOLD_GREEN,
            RED_GOLD_GREEN,
            RED_GREEN,
            GOLD_RED
    };

    public static BadSprite getForDay(int day) {
        if (day <= 0) return RED_GOLD_GREEN;
        return ALL[(day - 1) % ALL.length];
    }

    public static void init() {
        // Dummy init method to force class loading
    }
}
