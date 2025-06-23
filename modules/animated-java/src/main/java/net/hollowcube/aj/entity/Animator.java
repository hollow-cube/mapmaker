package net.hollowcube.aj.entity;

import net.hollowcube.aj.Animation;
import net.hollowcube.aj.animation.Channel;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface Animator {
    @NotNull Animator NOOP = new Noop();

    void tick();

    @NotNull Vec translation();

    @NotNull Vec scale();

    class AnimatorImpl implements Animator {

        private static class Keyframe {
            private final int tick;
            private Vec translation = Vec.ZERO;
            private Vec scale = Vec.ONE;

            public Keyframe(int tick) {
                this.tick = tick;
            }
        }

        private final List<Keyframe> keyframes = new ArrayList<>();
        private int tick = 0;

        public AnimatorImpl(@NotNull List<Animation.Keyframe> frames) {
            for (Animation.Keyframe frame : frames) {

                int tick = (int) (frame.time() * 20.0);
                Keyframe keyframe = getOrCreateKeyframe(tick);

                if (frame.channel() == Channel.POSITION)
                    keyframe.translation = frame.value().div(16);
                else if (frame.channel() == Channel.SCALE)
                    keyframe.scale = frame.value();
            }
        }

        private Keyframe getOrCreateKeyframe(int tick) {
            for (Keyframe keyframe : keyframes) {
                if (keyframe.tick == tick) {
                    return keyframe;
                }
            }
            Keyframe newKeyframe = new Keyframe(tick);
            keyframes.add(newKeyframe);
            return newKeyframe;
        }

        @Override
        public void tick() {
            tick++;
        }

        @Override
        public @NotNull Vec translation() {
            if (keyframes.isEmpty()) return Vec.ZERO;

            Keyframe prevKeyframe = keyframes.get(0);
            Keyframe nextKeyframe = null;

            for (Keyframe keyframe : keyframes) {
                if (keyframe.tick > tick) {
                    nextKeyframe = keyframe;
                    break;
                }
                prevKeyframe = keyframe;
            }

            // If there's no next keyframe, use only the previous one
            if (nextKeyframe == null) {
                return prevKeyframe.translation;
            }

            // Calculate interpolation factor
            float alpha = (float) (tick - prevKeyframe.tick) / (nextKeyframe.tick - prevKeyframe.tick);
            // Clamp alpha between 0 and 1
            alpha = Math.max(0, Math.min(1, alpha));

            // Linear interpolation between translations
            return prevKeyframe.translation.add(
                    nextKeyframe.translation.sub(prevKeyframe.translation).mul(alpha)
            );
        }

        @Override
        public @NotNull Vec scale() {
            if (keyframes.isEmpty()) return Vec.ONE;
            Keyframe lastKeyframe = keyframes.get(0);
            for (Keyframe keyframe : keyframes) {
                if (keyframe.tick > tick) {
                    break;
                }
                lastKeyframe = keyframe;
            }
            return lastKeyframe.scale;
        }
    }

    record Noop() implements Animator {

        @Override
        public void tick() {

        }

        @Override
        public @NotNull Vec translation() {
            return Vec.ZERO;
        }

        @Override
        public @NotNull Vec scale() {
            return Vec.ONE;
        }
    }

}
