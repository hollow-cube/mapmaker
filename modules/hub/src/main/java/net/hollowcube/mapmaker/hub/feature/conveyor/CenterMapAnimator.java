package net.hollowcube.mapmaker.hub.feature.conveyor;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class CenterMapAnimator extends AbstractAnimator {
    private static final Point SIMPLE_POS1 = new Vec(-57.5, 34, 55);
    private static final Point SIMPLE_POS2 = new Vec(-57.5, 34, 30);
    private static final Point SIMPLE_POS3 = new Vec(-57.5, 34, 14);
    private static final Point SIMPLE_POS4 = new Vec(-57.5, 34, -2);
    private static final Point SIMPLE_POS5 = new Vec(-57.5, 34, -18);
    private static final Point SIMPLE_POS6 = new Vec(-57.5, 34, -43);
    private static final Point SIMPLE_POS7 = new Vec(-57.5, 34, -66);

    private final Instance instance;

    public CenterMapAnimator(@NotNull Instance instance) {
        this.instance = instance;
    }

    @Override
    void loop() {
        while (true) {
            NpcItemModel mapModel = new NpcItemModel();
            // TODO(1.21.4)
//            mapModel.setModel(Material.STICK, 5);
            mapModel.getEntityMeta().setScale(new Vec(3.5));
            mapModel.getEntityMeta().setLeftRotation(new Quaternion(new Vec(1, 0, 0), Math.toRadians(-90)).into());

            FutureUtil.submitVirtual(() -> doSimplePathMove(mapModel));

            wait((int) ((20.0 / BLOCKS_PER_SECOND) * 1000));
        }
    }

    private void doSimplePathMove(@NotNull NpcItemModel mapModel) {
        var orientation = ThreadLocalRandom.current().nextInt(4) * 90;
        orientation += (int) ThreadLocalRandom.current().nextDouble(-10, 10);
        FutureUtil.getUnchecked(mapModel.setInstance(instance, new Pos(SIMPLE_POS7, orientation, 0)));

        moveToTarget(mapModel, SIMPLE_POS6);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS5);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS4);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS3);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS2);
        wait(100);
        moveToTarget(mapModel, SIMPLE_POS1);
        wait(100);
        delete(mapModel);
    }
}
