package net.hollowcube.mapmaker.hub.anim;

import net.hollowcube.mapmaker.hub.entity.util.InteractionEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class AnimationEntity extends Entity {

    private final List<Keyframe> keyframes = new ArrayList<>();
    private final boolean killOnFinish;
    private boolean once = false;

    public Runnable onReset;

    private int frameIndex = 0;
    private Keyframe curr = null; // Current frame
    private Keyframe next = null; // Next frame
    private long frameStart = -1; // Alive tick when the current frame started
    private long frameEnd = -1; // Alive tick when the current frame ends

    private final InterpolationHelper interp = new InterpolationHelper(this);

    private Entity interactionEntity = null;

    public AnimationEntity(@NotNull EntityType entityType, boolean killOnFinish) {
        super(entityType);
        this.killOnFinish = killOnFinish;

        setNoGravity(true);
        hasPhysics = false;
        collidesWithEntities = false;

        // If Minestom sends position sync, it will reset the interpolation breaking the animation
        setSynchronizationTicks(Integer.MAX_VALUE);
    }

    public void setKeyframes(@NotNull List<Keyframe> keyframes) {
        if (!keyframes.isEmpty()) {
            Check.argCondition(keyframes.getFirst().t() != 0, "First keyframe must be at t=0");
        }

        this.keyframes.clear();
        this.keyframes.addAll(keyframes);
        this.once = false;
        this.frameIndex = 0;
        this.curr = null;
        this.next = null;
        this.frameStart = -1;
        this.frameEnd = -1;
    }

    public void spawnHitbox() {

        var entityId = getEntityId();
        interactionEntity = new InteractionEntity(4, 5, 6, new InteractionEntity.Target() {
            @Override
            public void beginHover(@NotNull Player player) {
                // Enable glowing - This works because we never set any other flags in this set, otherwise
                // it would be overridden when sending other metadata changes.
                player.sendPacket(new EntityMetaDataPacket(entityId, Map.of(0, Metadata.Byte((byte) 0x40))));
            }

            @Override
            public void endHover(@NotNull Player player) {
                // Disable glowing - See above for how/why this is functional.
                player.sendPacket(new EntityMetaDataPacket(entityId, Map.of(0, Metadata.Byte((byte) 0x0))));
            }

            @Override
            public void onRightClick(@NotNull Player player) {
                player.sendMessage("you clicked one!!!");
            }
        });
        interactionEntity.setInstance(getInstance());
    }

    @Override
    public @NotNull AbstractDisplayMeta getEntityMeta() {
        return (AbstractDisplayMeta) super.getEntityMeta();
    }

    public void playOnce(@NotNull List<Keyframe> keyframes) {
        setKeyframes(keyframes);
        this.once = true;
        curr = keyframes.getFirst();
        movementTick();
    }

    long lastTime = -1;

    @Override
    protected void movementTick() {
        if (keyframes.isEmpty()) return;
        var meta = getEntityMeta();

        if (interactionEntity != null && getAliveTicks() % 4 == 0) {
            this.interactionEntity.refreshPosition(interp.getCurrentPosition(), false, true);
        }

        // If uninitialized, snap to init.
        if (curr == null) {
            meta.setNotifyAboutChanges(false);
            if (onReset != null) {
                onReset.run();
            }
            meta.setTransformationInterpolationStartDelta(0);
            meta.setPosRotInterpolationDuration(0);
            meta.setTransformationInterpolationDuration(0);
            meta.setNotifyAboutChanges(true);

            curr = keyframes.getFirst();
            for (var channel : Channel.values()) {
                curr.getOrDefault(channel).apply(interp);
            }
            return;
        }

        if (frameEnd > getAliveTicks()) {
            return;
        }

        if (frameIndex == keyframes.size() - 1) {
            if (killOnFinish) {
                remove();
                return;
            }

            frameIndex = 0;
            curr = null;
            if (once) keyframes.clear();
            return;
        }

        lastTime = System.currentTimeMillis();
        curr = keyframes.get(frameIndex);
        next = keyframes.get(frameIndex + 1);
        frameIndex++;
        frameStart = getAliveTicks();
        frameEnd = frameStart + (next.t() - curr.t());

        if (curr.onStart() != null) {
            curr.onStart().run();
        }

        var nextPosition = new AtomicReference<Channel.Value>();
        int interpDuration = next.t() - curr.t();
        if (!next.hasInterpolation()) interpDuration = 0;
        interp.beginInterpolation(interpDuration, () -> {
            for (var v : next.values().values()) {
                if (v.channel() == Channel.POSITION) {
                    nextPosition.set(v);
                    continue;
                }
                v.apply(interp);
            }
        });
        if (nextPosition.get() != null) {
            nextPosition.get().apply(interp);
        }
    }

    public @NotNull InterpolationHelper getInterp() {
        return interp;
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        this.interp.spawn(player);

        //todo sounds are complicated because they dont repeat. We would need to resend them constantly, per player
        // That seems not worth honestly. I think we should just have sounds for the actions like arms moving,
        // smashers smashing, etc.

//        player.playSound(Sound.sound(SoundEvent.ENTITY_MINECART_RIDING, Sound.Source.NEUTRAL, 1, 1), this);
    }

    @Override
    protected void remove(boolean permanent) {
        super.remove(permanent);
        if (permanent && interactionEntity != null)
            this.interactionEntity.remove();
    }
}
