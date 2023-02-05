package net.hollowcube.map.feature.experimental.marker;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Animation {
    private final List<Keyframe> keyframes;
    private final MarkerEntity entity;

    private int tick = 0;
    private int keyframeIndex = 0;

    public Animation(@NotNull List<Keyframe> keyframes, @NotNull MarkerEntity entity) {
        this.keyframes = new ArrayList<>(keyframes);
        this.entity = entity;
    }

    public void tick() {
        if (keyframeIndex >= keyframes.size() - 1) {
//            return; // Stop after one loop
            keyframeIndex = 0;
            tick = 0;
        }

        // Get current keyframe
        var keyframe = keyframes.get(keyframeIndex);
        var nextKeyframe = keyframes.get(keyframeIndex + 1);
        tick++;

        var easedT = keyframe.easing.ease(tick / (float) keyframe.durationTicks);

        var x = keyframe.x + (nextKeyframe.x - keyframe.x) * easedT;
        var y = keyframe.y + (nextKeyframe.y - keyframe.y) * easedT;
        var z = keyframe.z + (nextKeyframe.z - keyframe.z) * easedT;

        entity.setVelocity(new Vec(x, y, z));
        entity.refreshPosition(Pos.fromPoint(entity.origin().add(x, y, z)), true);

        if (tick >= keyframe.durationTicks) {
            keyframeIndex++;
            tick = 0;
        }
    }

    public record Keyframe(@NotNull EasingFunc easing, int durationTicks, double x, double y, double z) {

    }

    public interface EasingFunc {
        double ease(double t);

        @NotNull EasingFunc LINEAR = t -> t;
        @NotNull EasingFunc EASE_IN_SINE = t -> 1 - Math.cos(t * Math.PI / 2);
        @NotNull EasingFunc EASE_OUT_SINE = t -> Math.sin(t * Math.PI / 2);
        @NotNull EasingFunc EASE_IN_OUT_SINE = t -> -(Math.cos(Math.PI * t) - 1) / 2;
        @NotNull EasingFunc EASE_IN_QUINT = t -> t * t * t * t * t;
    }

}
