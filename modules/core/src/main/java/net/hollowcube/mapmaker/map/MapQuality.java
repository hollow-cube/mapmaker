package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;

public enum MapQuality {
    UNRATED,
    GOOD,
    GREAT,
    EXCELLENT,
    OUTSTANDING,
    MASTERPIECE;

    private static final MapQuality[] VALUES = values();

    public static final BadSprite EMPTY_STAR = BadSprite.require("icon/map_tooltip/star_empty");
    private static final BadSprite[] STARS = new BadSprite[]{
            BadSprite.require("icon/map_tooltip/star_0"),
            BadSprite.require("icon/map_tooltip/star_1"),
            BadSprite.require("icon/map_tooltip/star_2"),
            BadSprite.require("icon/map_tooltip/star_3"),
            BadSprite.require("icon/map_tooltip/star_4"),
            BadSprite.require("icon/map_tooltip/star_5"),
    };
    private static final BadSprite[] TOOLTIP_BORDERS = new BadSprite[]{
            BadSprite.require("icon/map_tooltip/quality_0"),
            BadSprite.require("icon/map_tooltip/quality_1"),
            BadSprite.require("icon/map_tooltip/quality_2"),
            BadSprite.require("icon/map_tooltip/quality_3"),
            BadSprite.require("icon/map_tooltip/quality_4"),
            BadSprite.require("icon/map_tooltip/quality_5"),
    };

    public static @NotNull MapQuality fromId(int id) {
        return VALUES[id];
    }

    public @NotNull BadSprite starSprite() {
        return STARS[this.ordinal()];
    }

    public @NotNull BadSprite tooltipBorderSprite() {
        return TOOLTIP_BORDERS[this.ordinal()];
    }
}
