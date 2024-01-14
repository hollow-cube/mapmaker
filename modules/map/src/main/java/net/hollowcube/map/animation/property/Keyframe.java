package net.hollowcube.map.animation.property;

import org.jetbrains.annotations.NotNull;

public class Keyframe<T> {
    private int time = 0;
    private T value;

    public int time() {
        return time;
    }

    public @NotNull T value() {
        return value;
    }

    /**
     * Updates the time, must be handled by the sequencer to ensure order remains correct.
     *
     * @param time The new time of this keyframe
     */
    void setTime(int time) {
        this.time = time;
    }

    public void setValue(T value) {
        this.value = value;
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
