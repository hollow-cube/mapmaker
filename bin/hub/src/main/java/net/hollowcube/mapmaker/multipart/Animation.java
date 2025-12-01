package net.hollowcube.mapmaker.multipart;

import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Vec;

import java.util.List;
import java.util.function.BiConsumer;

public class Animation {

    public record Animator(String id, BiConsumer<Bone, Vec> applicator, Keyframe[] keyframes) {

    }

    public record Keyframe(float time, Vec preTarget, Vec postTarget) {
        //todo interpolation field

    }

    private final float length;
    private final List<Animator> animators;

    private int tick = 0;

    public Animation(float length, List<Animator> animators) {
        this.length = length;
        this.animators = animators;
    }

    public void tick(Bone root) {
        float time = ((tick++) / (float) ServerFlag.SERVER_TICKS_PER_SECOND) % length;

//        System.out.println(time);

        for (var animator : animators) {
            var bone = root.findById(animator.id);
            // AJ allows having nonexistent bones, we just ignore them
            if (bone == null) continue;

            int currentIndex = Math.max(0, findIndex(animator.keyframes, time));
            int nextIndex = Math.min(animator.keyframes.length - 1, currentIndex + 1);

            Keyframe currentKeyframe = animator.keyframes[currentIndex];
            Keyframe nextKeyframe = animator.keyframes[nextIndex];
            float relativeTimestamp = time - currentKeyframe.time;
            float t;
            if (nextIndex != currentIndex) {
                t = Math.clamp(relativeTimestamp / (nextKeyframe.time - currentKeyframe.time), 0.0F, 1.0F);
            } else {
                t = 0.0F;
            }

            var result = currentKeyframe.postTarget.lerp(nextKeyframe.preTarget, t);
            animator.applicator.accept(bone, result);
        }

    }

    public void dump() {
        for (var animator : animators) {
            System.out.println(animator.id + ": " + animator.keyframes.length + " keyframes");
            for (var keyframe : animator.keyframes) {
                System.out.println("  " + keyframe.time + ": " + keyframe.preTarget + " -> " + keyframe.postTarget);
            }
        }
    }

    private int findIndex(Keyframe[] keyframes, float time) {
        for (int i = 0; i < keyframes.length; i++) {
            if (keyframes[i].time > time)
                return i - 1;
        }
        return keyframes.length - 1;
    }
}
