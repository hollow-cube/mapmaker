package net.hollowcube.canvas.internal.section.sprite;

import java.util.Map;

public record Sprite(char fontChar, int width, int offsetX) {
    //todo this will be generated automatically in the future
    public static Map<String, Sprite> SPRITE_MAP = Map.of(
            "gui/play_maps/container", new Sprite('\uEff8', 182, -11),
            "gui/play_maps/parkour_active", new Sprite('\uEff9', 54, -2)
    );
}
