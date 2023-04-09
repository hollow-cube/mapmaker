package net.hollowcube.canvas.internal.standalone.sprite;

import java.util.Map;

public record Sprite(char fontChar, int width, int offsetX) {
    //todo this will be generated automatically in the future
    public static Map<String, Sprite> SPRITE_MAP = Map.of(
            "gui/play_maps/container", new Sprite('\uEff8', 182, -11),
            "gui/play_maps/parkour_active", new Sprite('\uEff9', 54, -2),
            "gui/build_maps/container", new Sprite('\uEffa', 256, -8),
            "gui/build_maps/empty", new Sprite('\uEffb', 256, 71),
            "gui/build_maps/create", new Sprite('\uEffc', 256, 71),
            "gui/build_maps/edit", new Sprite('\uEffd', 256, 71),
            "gui/build_maps/details", new Sprite('\uEffe', 256, -8)
    );
}
