package net.hollowcube.mapmaker.hub.feature.conveyor;

import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.packet.server.play.BundlePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class AbstractAnimator {

    protected final double BLOCKS_PER_SECOND = 2.5;

    abstract void loop();

    protected void delete(NpcItemModel entity) {
        entity.remove();
    }

    protected void rotateYaw(NpcItemModel entity, float degrees, int time) {
        rotateYaw(entity, degrees, time, true);
    }

    protected void rotateYaw(NpcItemModel entity, float degrees, int time, boolean wait) {
        //todo compute the speed based on the blocks/second speed or just another constant for rotational velocity
        var meta = entity.getEntityMeta();

        meta.setNotifyAboutChanges(false);

        meta.setTransformationInterpolationStartDelta(0);
        meta.setPosRotInterpolationDuration(time / 50);

        meta.setNotifyAboutChanges(true);
        entity.teleport(entity.getPosition().withYaw(degrees)).join();

        if (wait) wait(time);
    }

    protected void setPivotPos(NpcItemModel entity, @NotNull Point newPivot, @Nullable Point teleportPos) { //todo i think teleportPos can be computed but im lazy
        // Translate away from the pivot and teleport to it with no interpolation

        entity.sendPacketsToViewers(new BundlePacket());
        var meta = entity.getEntityMeta();
        meta.setNotifyAboutChanges(false);

        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(0);
        meta.setTranslation(entity.getPosition().sub(newPivot.withY(entity.getPosition().y())));
        meta.setPosRotInterpolationDuration(0);

        meta.setNotifyAboutChanges(true);
        entity.teleport(new Pos(teleportPos != null ? teleportPos : newPivot).withView(entity.getPosition())).join();
        entity.sendPacketsToViewers(new BundlePacket());
    }

    protected int moveToTarget(NpcItemModel entity, @NotNull Point target) {
        return moveToTarget(entity, target, true);
    }

    protected int moveToTarget(NpcItemModel entity, @NotNull Point target, boolean wait) {
        var currentPos = entity.getPosition();
        var distance = currentPos.distance(target);

        var time = (int) (distance / BLOCKS_PER_SECOND * 1000.0);
        var modelMeta = entity.getEntityMeta();
        modelMeta.setPosRotInterpolationDuration(time / 50);
        entity.teleport(new Pos(target).withView(entity.getPosition()));
        if (wait) wait(time);
        return time;
    }

    protected void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
