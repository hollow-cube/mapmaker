package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
public class CyberpunkTrainFeature implements HubFeature {

    private static final Point TRAIN_START = new Vec(-67.5, 75, -55.6);
    private static final Point TRAIN_END = new Vec(-150.5, 75, -97.2);

    private final NpcItemModel trainFront = new NpcItemModel();
    private final NpcItemModel trainMiddle = new NpcItemModel();
    private final NpcItemModel trainBack = new NpcItemModel();
    //todo it would be nice to have a "group" entity which controls these ones.

    private boolean moving = false;

    @Inject
    public CyberpunkTrainFeature(@NotNull HubMapWorld world, @NotNull Scheduler scheduler) {
        trainFront.setModel(Material.STICK, 7);
        trainFront.getEntityMeta().setScale(new Vec(4));
        var metaFront = trainFront.getEntityMeta();
        metaFront.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(-90)).into());
        metaFront.setTranslation(new Vec(-10, 0, 1));
        metaFront.setBrightnessOverride(240);
        trainFront.setInstance(world.instance(), new Pos(TRAIN_START, 27.5f, 0)).join();

        trainMiddle.setInstance(world.instance(), new Vec(0, 40, 0)).join();
        trainMiddle.setModel(Material.STICK, 6);
        trainMiddle.getEntityMeta().setScale(new Vec(4));
        var metaMiddle = trainMiddle.getEntityMeta();
        metaMiddle.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(-90)).into());
        metaMiddle.setBrightnessOverride(240);
        trainMiddle.setInstance(world.instance(), new Pos(TRAIN_START, 27.5f, 0)).join();

        trainBack.setInstance(world.instance(), new Vec(0, 40, 0)).join();
        trainBack.setModel(Material.STICK, 7);
        trainBack.getEntityMeta().setScale(new Vec(4));
        var metaBack = trainBack.getEntityMeta();
        metaBack.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(-90)).into());
        metaBack.setTranslation(new Vec(-5, 0, -1));
        metaBack.setBrightnessOverride(240);
        trainBack.setInstance(world.instance(), new Pos(TRAIN_START, -180 + 26f, 0)).join();

        scheduler.submitTask(this::trainUpdate);
    }

    private @NotNull TaskSchedule trainUpdate() {
        if (moving) {
            trainFront.sendPacketToViewers(new BundlePacket());
            setPosInterpolation(0);
            trainFront.teleport(new Pos(TRAIN_START).withView(trainFront.getPosition()));
            trainMiddle.teleport(new Pos(TRAIN_START).withView(trainMiddle.getPosition()));
            trainBack.teleport(new Pos(TRAIN_START).withView(trainBack.getPosition()));
            trainFront.sendPacketToViewers(new BundlePacket());
            moving = false;
            return TaskSchedule.tick(60);
        } else {
            trainFront.sendPacketToViewers(new BundlePacket());
            setPosInterpolation(30);
            trainFront.teleport(new Pos(TRAIN_END).withView(trainFront.getPosition()));
            trainMiddle.teleport(new Pos(TRAIN_END).withView(trainMiddle.getPosition()));
            trainBack.teleport(new Pos(TRAIN_END).withView(trainBack.getPosition()));
            trainFront.sendPacketToViewers(new BundlePacket());
            moving = true;
            return TaskSchedule.tick(45);
        }
    }

    private void setPosInterpolation(int duration) {
        trainFront.getEntityMeta().setPosRotInterpolationDuration(duration);
        trainMiddle.getEntityMeta().setPosRotInterpolationDuration(duration);
        trainBack.getEntityMeta().setPosRotInterpolationDuration(duration);
    }

}
