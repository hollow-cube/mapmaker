package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.mapmaker.util.TagUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.tag.Tag;

import java.util.function.Function;

public class CheckpointSetting {

    /** Reset height for the checkpoint, or -1 if unset. */
    public static final Tag<Integer> RESET_HEIGHT = Tag.Integer("reset_height").defaultValue(-1);
    public static final Function<Integer, Component> RESET_HEIGHT_TEXT_FUNCTION = resetHeight -> resetHeight == -1
            ? Component.text("None", TextColor.color(0xFF2D2D))
            : Component.text(resetHeight);
    public static final Tag<Component> RESET_HEIGHT_TEXT = CheckpointSetting.RESET_HEIGHT
            .map(RESET_HEIGHT_TEXT_FUNCTION, TagUtil::noop);

    private CheckpointSetting() {
    }
}
