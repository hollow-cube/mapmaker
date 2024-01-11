package net.hollowcube.mapmaker.hub.feature.conveyor;

import com.google.auto.service.AutoService;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BundlePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoService(HubFeature.class)
public class MapConveyorFeature implements HubFeature {
    // Simple path on spawn left
    private static final Point SIMPLE_POS1 = new Vec(-71.5, 37, 85.5);
    private static final Point SIMPLE_POS2 = new Vec(-71.5, 37, 50.5);
    private static final Point SIMPLE_POS3 = new Vec(-71.5, 37, 32.5);
    private static final Point SIMPLE_POS4 = new Vec(-91.5, 37, 32.5);
    private static final Point SIMPLE_POS5 = new Vec(-91.5, 37, 4.5);

    // Complex path on spawn left
//    private static final Point POS1 = new Vec(-71.5, 37, 45.5);
    private static final Point POS1 = new Vec(-71.5, 37, 85.5);
    private static final Point POS1_1 = new Vec(-71.5, 37, 50.5);
    private static final Point POS2 = new Vec(-71.5, 37, 38.5);
    private static final Point POS3 = new Vec(-64.5, 37, 22.5);
    private static final Point POS4 = new Vec(-81.5, 37, 22.5);
    private static final Point POS5 = new Vec(-81.5, 37, 4.5);

    private static final Point CLAW_PIVOT = new Vec(-64.5, 37, 38.5);

    private final double BLOCKS_PER_SECOND = 2.5;


    private final NpcItemModel clawPillar = new NpcItemModel();
    private final NpcItemModel clawClaw = new NpcItemModel();

    @Override
    public void init(@NotNull HubServer hub) {

        clawPillar.setModel(Material.STICK, 9);
        clawPillar.getEntityMeta().setScale(new Vec(16));
        clawPillar.setInstance(hub.instance(), new Pos(CLAW_PIVOT, 180, 0)).join();

        clawClaw.setModel(Material.STICK, 10);
        clawClaw.getEntityMeta().setScale(new Vec(16));
        clawClaw.setInstance(hub.instance(), new Pos(CLAW_PIVOT.withY(y -> y + 5), 180, 0)).join();

        FutureUtil.submitVirtual(() -> {
            int i = 0;
            while (true) {
                NpcItemModel mapModel = new NpcItemModel();
                mapModel.setModel(Material.STICK, 5);
                mapModel.getEntityMeta().setScale(new Vec(3.5));
                mapModel.setInstance(hub.instance(), POS1).join();
                mapModel.getEntityMeta().setLeftRotation(new Quaternion(new Vec(1, 0, 0), Math.toRadians(-90)).into());

                if (i++ % 2 == 0) {
                    FutureUtil.submitVirtual(() -> doComplexPathMove(mapModel));
                } else {
                    FutureUtil.submitVirtual(() -> doSimplePathMove(mapModel));
                }

                wait((int) ((20.0 / BLOCKS_PER_SECOND) * 1000));
            }
        });
    }

    private void doSimplePathMove(@NotNull NpcItemModel mapModel) {
        moveToTarget(mapModel, SIMPLE_POS1);
        moveToTarget(mapModel, SIMPLE_POS2);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS3);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS4);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS5);
        wait(100);
        delete(mapModel);
    }

    private void doComplexPathMove(@NotNull NpcItemModel mapModel) {
        //todo use bundles for all of these

        moveToTarget(mapModel, POS1_1);
        moveToTarget(mapModel, POS2);
        wait(100);

        // Adjust pivot to claw pivot
        setPivotPos(mapModel, new Vec(-64.5, 37, 38.5), null);
        wait(50);

        // Move claw down
        moveToTarget(clawClaw, clawClaw.getPosition().withY(y -> y - 5));
        wait(100);

        // Move claw AND map up
        moveToTarget(mapModel, mapModel.getPosition().withY(y -> y + 5), false);
        moveToTarget(clawClaw, clawClaw.getPosition().withY(y -> y + 5), true);
        wait(100);

        var radius = 7.0;
        var arcLength = (2 * Math.PI * radius) * (90.0 / 360);
        // Speed up the rotation slightly here to make it plausible to do every other box
        var rotationTime = (int) ((arcLength / BLOCKS_PER_SECOND) * 1000.0 * 0.75);

        // Rotate the claw AND map
        rotateYaw(mapModel, 90, rotationTime, false);
        rotateYaw(clawClaw, -90, rotationTime, false);
        rotateYaw(clawPillar, -90, rotationTime, true);
        wait(100);

        // Move clan AND map down
        moveToTarget(mapModel, mapModel.getPosition().withY(y -> y - 5), false);
        moveToTarget(clawClaw, clawClaw.getPosition().withY(y -> y - 5), true);

        FutureUtil.submitVirtual(() -> {
            // Move the claw back up
            moveToTarget(clawClaw, clawClaw.getPosition().withY(y -> y + 5));
            wait(100);

            // Rotate the claw back
            rotateYaw(clawClaw, 180, rotationTime, false);
            rotateYaw(clawPillar, 180, rotationTime, true);
            wait(100);

        });

        // Reset the pivot back to expected
        setPivotPos(mapModel, mapModel.getPosition(), new Vec(-64.5, 37, 31.5));
        wait(100);

        // Finish the path
        moveToTarget(mapModel, POS3);
        wait(100);
        moveToTarget(mapModel, POS4);
        wait(100);
        moveToTarget(mapModel, POS5);

        delete(mapModel);
    }

    private void delete(NpcItemModel entity) {
        entity.remove();
    }

    private void rotateYaw(NpcItemModel entity, float degrees, int time) {
        rotateYaw(entity, degrees, time, true);
    }

    private void rotateYaw(NpcItemModel entity, float degrees, int time, boolean wait) {
        //todo compute the speed based on the blocks/second speed or just another constant for rotational velocity
        var meta = entity.getEntityMeta();

        meta.setNotifyAboutChanges(false);

        meta.setTransformationInterpolationStartDelta(0);
        meta.setPosRotInterpolationDuration(time / 50);

        meta.setNotifyAboutChanges(true);
        entity.teleport(entity.getPosition().withYaw(degrees)).join();

        if (wait) wait(time);
    }

    private void setPivotPos(NpcItemModel entity, @NotNull Point newPivot, @Nullable Point teleportPos) { //todo i think teleportPos can be computed but im lazy
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

    private int moveToTarget(NpcItemModel entity, @NotNull Point target) {
        return moveToTarget(entity, target, true);
    }

    private int moveToTarget(NpcItemModel entity, @NotNull Point target, boolean wait) {
        var currentPos = entity.getPosition();
        var distance = currentPos.distance(target);

        var time = (int) (distance / BLOCKS_PER_SECOND * 1000.0);
        var modelMeta = entity.getEntityMeta();
        modelMeta.setPosRotInterpolationDuration(time / 50);
        entity.teleport(new Pos(target).withView(entity.getPosition()));
        if (wait) wait(time);
        return time;
    }

    private void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
