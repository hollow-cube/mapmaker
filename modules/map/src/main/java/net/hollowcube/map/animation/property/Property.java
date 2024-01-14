package net.hollowcube.map.animation.property;

import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public record Property<T>(
        @NotNull String name,
        @NotNull T defaultValue,
        boolean isServerInterpolated,
        @NotNull Interpolator<T> interpolator,
        @NotNull BiConsumer<Entity, T> applier
) {

    public interface Interpolator<T> {
        T interpolate(@NotNull T from, @NotNull T to, float progress);
    }

    /**
     * Returns true if the property must be interpolated on the server. This will result in it being updated every
     * single tick with new progress, as opposed to just once a keyframe is reached/on play/pause.
     *
     * @return True if the property must be interpolated on the server
     */
//    public abstract boolean isServerInterpolated();
//
//    public abstract @NotNull T defaultValue();
//
    public void apply(@NotNull Entity entity, @NotNull T from, @NotNull T to, float progress) {
        applier.accept(entity, interpolator.interpolate(from, to, progress));
    }

    // Property types
    // - Vec3f (position, translation, scale, color)
    // - Vec2f (rotation (yaw, pitch))
    // - Quaternion (rotation (transform))
    // - float (opacity)

    // FOR DISPLAY ENTITY INTERPOLATION
    // - POS / ROT INTERP -- position, yaw/pitch
    // - TRANSFORM INTERP -- EVERYTHING ELSE


    // There are a few types of property:
    // - static/only change exactly on the keyframe (eg: on fire or not, display text, item model).
    // - client interpolated (eg: display entity fields). these also only need to be notified of change on the keyframe because we send the update once.
    // - server interpolated (eg: armor stand arms, position of non-display entities). these must be notified of change every tick.

}
