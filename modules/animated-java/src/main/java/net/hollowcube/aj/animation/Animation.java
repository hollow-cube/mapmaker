package net.hollowcube.aj.animation;

import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public record Animation(
        @NotNull LoopMode loop,
        int duration, // ticks
        @NotNull Map<UUID, Animator> animators
) {

    public record Animator(
            @NotNull EnumMap<Channel, Keyframe[]> keyframes
    ) {

    }
}
