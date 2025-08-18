package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
public class CyberpunkTrainFeature implements HubFeature {

    private static final Point TRAIN_START = new Vec(-67.5, 75, -55.6);
    private static final Vec TRAIN_OFFSET = new Vec(-100, 0, 0);

    private static final Vec FRONT_TRANSLATION = new Vec(-10, 0, 1);
    private static final Vec MIDDLE_TRANSLATION = Vec.ZERO;
    private static final Vec END_TRANSLATION = new Vec(-5, 0, -1);


    private final NpcItemModel trainFront = new NpcItemModel();
    private final NpcItemModel trainMiddle = new NpcItemModel();
    private final NpcItemModel trainBack = new NpcItemModel();
    //todo it would be nice to have a "group" entity which controls these ones.

    private boolean moving = false;

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        trainFront.setModel(Material.STICK, BadSprite.require("train_front"));
        trainFront.getEntityMeta().setScale(new Vec(4));
        var metaFront = trainFront.getEntityMeta();
        metaFront.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(-90)).into());
        metaFront.setTranslation(FRONT_TRANSLATION);
        trainFront.setInstance(world.instance(), new Pos(TRAIN_START, 27.5f, 0));

        trainMiddle.setModel(Material.STICK, BadSprite.require("train_middle"));
        trainMiddle.getEntityMeta().setScale(new Vec(4));
        var metaMiddle = trainMiddle.getEntityMeta();
        metaMiddle.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(-90)).into());
        trainMiddle.setInstance(world.instance(), new Pos(TRAIN_START, 27.5f, 0));

        trainBack.setModel(Material.STICK, BadSprite.require("train_front"));
        trainBack.getEntityMeta().setScale(new Vec(4));
        var metaBack = trainBack.getEntityMeta();
        metaBack.setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(-90)).into());
        metaBack.setTranslation(END_TRANSLATION);
        trainBack.setInstance(world.instance(), new Pos(TRAIN_START, -180 + 26f, 0));

        server.scheduler().submitTask(this::trainUpdate);
    }

    private @NotNull TaskSchedule trainUpdate() {
        if (moving) {
            moving = false;
            trainFront.editEntityMeta(meta -> {
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTransformationInterpolationDuration(0);
                meta.setTranslation(FRONT_TRANSLATION);
            });
            trainMiddle.editEntityMeta(meta -> {
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTransformationInterpolationDuration(0);
                meta.setTranslation(MIDDLE_TRANSLATION);
            });
            trainBack.editEntityMeta(meta -> {
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTransformationInterpolationDuration(0);
                meta.setTranslation(END_TRANSLATION);
            });
            return TaskSchedule.tick(60);
        } else {
            moving = true;
            trainFront.editEntityMeta(meta -> {
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTransformationInterpolationDuration(30);
                meta.setTranslation(FRONT_TRANSLATION.add(TRAIN_OFFSET));
            });
            trainMiddle.editEntityMeta(meta -> {
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTransformationInterpolationDuration(30);
                meta.setTranslation(MIDDLE_TRANSLATION.add(TRAIN_OFFSET));
            });
            trainBack.editEntityMeta(meta -> {
                meta.setTransformationInterpolationStartDelta(0);
                meta.setTransformationInterpolationDuration(30);
                meta.setTranslation(END_TRANSLATION.sub(TRAIN_OFFSET));
            });
            return TaskSchedule.tick(45);
        }
    }

}
