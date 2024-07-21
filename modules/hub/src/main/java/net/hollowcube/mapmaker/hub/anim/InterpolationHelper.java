package net.hollowcube.mapmaker.hub.anim;

import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.network.packet.server.play.EntityHeadLookPacket;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.network.packet.server.play.EntityTeleportPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@SuppressWarnings("UnstableApiUsage")
public class InterpolationHelper {

    private final Entity entity;
    private final AbstractDisplayMeta meta;

    private int interpolationStart = -1;
    private int interpolationDuration = -1;
    private int lastUpdate = -1;
    private float t = 0;

    // lastXXX Is the constant value
    // if nextXXX is not null then we are mid interpolation on the current time
    private Pos lastPosition = Pos.ZERO;
    private Pos nextPosition;
    private Point lastTranslation = Vec.ZERO;
    private Point nextTranslation;
//    private Point lastScale = Vec.ZERO;
//    private Point nextScale;
//    private Quaternion lastLeftRotation = new Quaternion(0, 0, 0, 0);
//    private Quaternion nextLeftRotation;
//    private Quaternion lastRightRotation = new Quaternion(0, 0, 0, 0);
//    private Quaternion nextRightRotation;

    public InterpolationHelper(@NotNull Entity entity) {
        this.entity = entity;
        this.meta = (AbstractDisplayMeta) entity.getEntityMeta();
    }

    public void beginInterpolation(int duration, Runnable setup) {
        getProgress(); // Update internal state
        if (interpolationStart != -1) updateState();
//        Check.stateCondition(interpolationStart != -1, "Interpolation already started");
        this.interpolationStart = duration <= 0 ? -1 : (int) (entity.getAliveTicks() + 1);
        this.interpolationDuration = duration <= 0 ? -1 : duration;
        this.lastUpdate = -1;
        this.t = -1;

        meta.setNotifyAboutChanges(false);
        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(duration);
        meta.setPosRotInterpolationDuration(duration);
        setup.run();
        meta.setNotifyAboutChanges(true);
    }

    private float getProgress() {
        if (lastUpdate >= entity.getAliveTicks()) return this.t;

        this.lastUpdate = (int) entity.getAliveTicks();
        this.t = (entity.getAliveTicks() - interpolationStart) / (float) interpolationDuration;
        if (interpolationStart + interpolationDuration <= entity.getAliveTicks()) {
            this.interpolationStart = -1;
            this.interpolationDuration = -1;
            this.t = 1;

            // Set all of the next values to current and null
            updateState();
        }

        return this.t;
    }

    private void updateState() {
        if (nextPosition != null) {
            lastPosition = nextPosition;
            nextPosition = null;
        }
        if (nextTranslation != null) {
            lastTranslation = nextTranslation;
            nextTranslation = null;
        }
    }

    public @NotNull Pos getCurrentPosition() {
        float t = getProgress();
        if (nextPosition == null || t <= Vec.EPSILON) return lastPosition;
        return CoordinateUtil.lerp(lastPosition, nextPosition, t);
    }

    public void setPosition(@NotNull Pos newPosition) {
        if (interpolationStart == -1) {
            // No interpolation active, update immediately
            this.lastPosition = newPosition;
            this.nextPosition = null;
        } else {
            this.nextPosition = newPosition;
        }
        entity.refreshPosition(newPosition, false, true);
    }

    public @NotNull Point getCurrentTranslation() {
        float t = getProgress();
        if (nextTranslation == null || t <= Vec.EPSILON) return lastTranslation;
        return CoordinateUtil.lerp(lastTranslation, nextTranslation, t);
    }

    public void setTranslation(@NotNull Point newTranslation) {
        if (interpolationStart == -1) {
            // No interpolation active, update immediately
            this.lastTranslation = newTranslation;
            this.nextTranslation = null;
        } else {
            this.nextTranslation = newTranslation;
        }
        this.meta.setTranslation(newTranslation);
    }

    public boolean isInterpolating() {
        return interpolationStart != -1;
    }

    private @NotNull EntityMetaDataPacket getCurrentMetadataPacket() {
        var entries = new HashMap<>(entity.getMetadataPacket().entries());
        entries.remove(AbstractDisplayMeta.OFFSET + 0); // interp start delta
        entries.remove(AbstractDisplayMeta.OFFSET + 1); // transform duration
        entries.remove(AbstractDisplayMeta.OFFSET + 2); // posrot duration
        entries.put(AbstractDisplayMeta.OFFSET + 3, Metadata.Vector3(getCurrentTranslation()));
        return new EntityMetaDataPacket(entity.getEntityId(), entries);
    }

    private @NotNull EntityMetaDataPacket getNextMetadataPacket() {
        var entries = new HashMap<>(entity.getMetadataPacket().entries());
        var remaining = interpolationDuration - (entity.getAliveTicks() - interpolationStart);
        entries.put(AbstractDisplayMeta.OFFSET + 0, Metadata.VarInt(0)); // interp start delta
        entries.put(AbstractDisplayMeta.OFFSET + 1, Metadata.VarInt((int) remaining)); // transform duration
        entries.put(AbstractDisplayMeta.OFFSET + 2, Metadata.VarInt((int) remaining)); // posrot duration
        return new EntityMetaDataPacket(entity.getEntityId(), entries);
    }

    /**
     * Spawn the entity at its current (interpolated) position
     */
    public void spawn(@NotNull Player player) {
        var spawnPosition = getCurrentPosition();
        // If at zero, start at the spawn position. This can happen if the entity is spawned before a tick.
        if (spawnPosition.isZero()) spawnPosition = entity.getPosition();

        player.sendPacket(new SpawnEntityPacket(entity.getEntityId(), entity.getUuid(), entity.getEntityType().id(),
                spawnPosition, spawnPosition.yaw(), 0, (short) 0, (short) 0, (short) 0));
        player.sendPacket(getCurrentMetadataPacket());
        player.sendPacket(new EntityHeadLookPacket(entity.getEntityId(), spawnPosition.yaw())); //todo is this necessary?

        if (!isInterpolating()) return;

        player.scheduleNextTick(_ -> {
            player.sendPacket(getNextMetadataPacket());
            if (nextPosition != null) {
                player.sendPacket(new EntityTeleportPacket(entity.getEntityId(), nextPosition, entity.isOnGround()));
            }
        });
    }

}
