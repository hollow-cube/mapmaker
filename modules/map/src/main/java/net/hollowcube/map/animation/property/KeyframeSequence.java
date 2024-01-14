package net.hollowcube.map.animation.property;

import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sequence represents the keyframes for a single property of an object.
 */
public class KeyframeSequence<T> {
    public final Property<T> property;
    public List<Keyframe<T>> keyframes = new ArrayList<>();

    private Keyframe<T> current = null;
    private Keyframe<T> next = null;
    private int t = 0;

    public KeyframeSequence(@NotNull Property<T> property) {
        this.property = property;

        var zero = new Keyframe<T>();
        zero.setTime(0);
        zero.setValue(property.defaultValue());
        keyframes.add(zero);
    }

    public void seek(int tick) {
        for (int i = 0; i < keyframes.size(); i++) {
            var keyframe = keyframes.get(i);
            if (keyframe.time() <= tick) {
                current = keyframe;
                next = i + 1 < keyframes.size() ? keyframes.get(i + 1) : null;
            } else break;
        }
        t = tick;
    }

    public void sync(@NotNull Entity entity, @NotNull Keyframe<?> keyframe) {
        interpolate(entity, (Keyframe<T>) keyframe, (Keyframe<T>) keyframe, 0);
    }

    public void sync(@NotNull Entity entity) {
        if (next == null) {
            interpolate(entity, current, current, 0);
        } else {
            float progress = ((t - current.time()) / (float) (next.time() - current.time()));
            interpolate(entity, current, next, progress);
        }
    }

    public void step(@NotNull Entity entity) {
        seek(t + 1);

        if (next == null) {
            // We've reached the end of the animation, but if it is the first tick of the final frame
            // we should ensure that we are synced to that position exactly.
            interpolate(entity, current, current, 0);
//            pause();
            return;
        }

        float progress = ((t - current.time()) / (float) (next.time() - current.time()));
        interpolate(entity, current, next, progress);
    }

    public @Nullable Keyframe<?> next(@NotNull Keyframe<?> current) {
        for (int i = 0; i < keyframes.size(); i++) {
            var keyframe = keyframes.get(i);
            if (keyframe == current) {
                return i + 1 < keyframes.size() ? keyframes.get(i + 1) : null;
            }
        }
        return null;
    }

    public @NotNull Keyframe<T> keyframe(int time, boolean exact) {
        for (int i = 0; i < keyframes.size(); i++) {
            var keyframe = keyframes.get(i);
            if (keyframe.time() == time) {
                return keyframe;
            }
            // If not exact and this keyframe is past the tick, we should return the previous one
            if (!exact && keyframe.time() > time) {
                return keyframes.get(i - 1);
            }

            if (exact && keyframe.time() > time) {
                var newKeyframe = new Keyframe<T>();
                newKeyframe.setTime(time);
                keyframes.add(i, newKeyframe);
                System.out.println("inserted keyframe at t=" + time);
                return newKeyframe;
            }
        }

        if (exact) {
            var newKeyframe = new Keyframe<T>();
            newKeyframe.setTime(time);
            keyframes.add(newKeyframe);
            System.out.println("inserted keyframe at t=" + time);
            return newKeyframe;
        } else {
            // Return last
            return keyframes.get(keyframes.size() - 1);
        }
    }

    public @NotNull Keyframe<T> interpolate(@NotNull Keyframe<?> other, float progress) {
        var newKeyframe = new Keyframe<T>();
        newKeyframe.setTime((int) (current.time() + (next.time() - current.time()) * progress));
        newKeyframe.setValue(property.interpolator().interpolate(current.value(), (T) other.value(), progress));
        return newKeyframe;
    }


    private void interpolate(@NotNull Entity entity, @NotNull Keyframe<T> from, @NotNull Keyframe<T> to, float progress) {
        property.apply(entity, from.value(), to.value(), progress);
    }


}
