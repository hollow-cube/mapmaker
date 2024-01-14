package net.hollowcube.map.animation.animator;

import net.hollowcube.map.animation.Animator;
import net.hollowcube.map.animation.property.Keyframe;
import net.hollowcube.map.animation.property.KeyframeSequence;
import net.hollowcube.map.animation.property.Property;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityMetadataStealer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class AnimatorV2 implements Animator {
    private final Map<Property<?>, KeyframeSequence<?>> keyframes = new HashMap<>();
    private final List<KeyframeSequence<?>> serverInterpolatedKeyframes = new ArrayList<>();
    private final List<KeyframeSequence<?>> clientInterpolatedKeyframes = new ArrayList<>();
    private int t;

    private Instance instance = null;
    private Entity entity = null;

    public AnimatorV2(@NotNull Instance instance, @NotNull Entity entity, int t) {
        this.instance = instance;
        this.entity = new AnimatingEntity(entity);
        this.t = t;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void seek(int tick) {
        Check.argCondition(tick < 0, "tick cannot be negative");

        if (entity.getEntityMeta() instanceof AbstractDisplayMeta meta) {
            meta.setNotifyAboutChanges(false);
            meta.setPosRotInterpolationDuration(0);
            meta.setTransformationInterpolationDuration(0);
        }

        this.t = tick;
        for (var sequence : keyframes.values()) {
            sequence.seek(tick);
        }
        timeToNextClientFrame = -1;

        if (entity.getEntityMeta() instanceof AbstractDisplayMeta meta) {
            meta.setNotifyAboutChanges(true);
        }

    }

    public void sync() {
        for (var sequence : keyframes.values()) {
            sequence.sync(entity);
        }
    }

    @Override
    public void play() {

    }

    @Override
    public void pause() {

    }

    public boolean hasProperty(@NotNull Property<?> property) {
        return keyframes.containsKey(property);
    }

    public <T> @NotNull Keyframe<T> keyframe(@NotNull Property<T> property) {
        return keyframe(property, true);
    }

    public <T> @NotNull Keyframe<T> keyframe(@NotNull Property<T> property, boolean exact) {
        var sequence = keyframes.get(property);
        if (sequence == null) {
            sequence = new KeyframeSequence<>(property);
            if (property.isServerInterpolated()) {
                serverInterpolatedKeyframes.add(sequence);
            } else {
                clientInterpolatedKeyframes.add(sequence);
            }
            keyframes.put(property, sequence);
        }
        //noinspection unchecked
        return (Keyframe<T>) sequence.keyframe(t, exact);
    }

    protected int t() {
        return t;
    }

    public Map<Property<?>, KeyframeSequence<?>> getKeyframes() {
        return keyframes;
    }

    private int timeToNextClientFrame = -1;

    @Override
    public void tick() {
        for (var sequence : serverInterpolatedKeyframes) {
            sequence.step(entity);
        }

        if (--timeToNextClientFrame <= 0) {
            int timeToNext = Integer.MAX_VALUE;

            List<KeyframeSequence<Object>> properties = new ArrayList<>();
            List<Keyframe<?>> currents = new ArrayList<>();
            List<Keyframe<?>> nexts = new ArrayList<>();

            for (var sequence : clientInterpolatedKeyframes) {
                // Get the current keyframe in the sequence as well as the time until the next keyframe
                var current = sequence.keyframe(t, false);
                var next = sequence.next(current);
                if (next == null) {
                    // End of sequence, just sync to the current keyframe
                    sequence.sync(entity, current);
                    continue;
                }

                timeToNext = Math.min(timeToNext, next.time() - t);
                properties.add((KeyframeSequence<Object>) sequence);
                currents.add(current);
                nexts.add(next);
            }

            // Now that we have the min time to next, we need to create synthetic keyframes for all of the sequences
            // that don't have a keyframe at the next tick.
            int nextTick = t + timeToNext;

            entity.sendPacketsToViewers(new BundlePacket());
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta meta) {
                meta.setNotifyAboutChanges(false);
                meta.setPosRotInterpolationDuration(timeToNext);
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTransformationInterpolationDuration(timeToNext);
                meta.setNotifyAboutChanges(true);
            }

            for (int i = 0; i < currents.size(); i++) {
                var sequence = properties.get(i);
                var current = currents.get(i);
                var next = nexts.get(i);

                if (current.time() != t) {
                    current = sequence.interpolate(next, (t - current.time()) / (float) (next.time() - current.time()));
                }
                if (next.time() != nextTick) {
                    next = sequence.interpolate(next, (nextTick - current.time()) / (float) (next.time() - current.time()));
                }


                sequence.property.applier().accept(entity, next.value());
//                sequence.property.apply(entity, current.value(), next.value(), 0);
            }

            entity.sendPacketsToViewers(new BundlePacket());

            timeToNextClientFrame = timeToNext;
        }

        t++;
    }

    private class AnimatingEntity extends Entity {

        public AnimatingEntity(@NotNull Entity copyFrom) {
            super(copyFrom.getEntityType());
            metadata = EntityMetadataStealer.steal(copyFrom);

            setNoGravity(true);
            hasPhysics = false;
        }

        public AnimatingEntity(@NotNull EntityType entityType) {
            super(entityType);

            setNoGravity(true);
            hasPhysics = false;
        }

        @Override
        public void update(long time) {
            super.update(time);

//            if (ticking) EntityAnimator.this.step();
        }
    }
}
