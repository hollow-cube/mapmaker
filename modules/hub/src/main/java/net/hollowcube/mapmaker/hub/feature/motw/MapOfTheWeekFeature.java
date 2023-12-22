package net.hollowcube.mapmaker.hub.feature.motw;

import com.google.auto.service.AutoService;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.item.Material;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
public class MapOfTheWeekFeature implements HubFeature {

    private static final Pos MAP_ENTITY_POS = new Pos(-38 + 0.5, 43, 54 + 0.5, 0, -90);
    private static final int MAP_ENTITY_UPDATE_INTERVAL = 5; // Seconds

    private final NpcItemModel mapEntity = new NpcItemModel();
    private int mapEntityRotationTarget = 0;

    @Override
    public void init(@NotNull HubServer hub) {
        // Timer init (the big countdown above the map)
        var timer = new CountdownTimer(hub.instance());
        hub.scheduler().submitTask(timer, ExecutionType.SYNC);

        // Spinning map
        mapEntity.setModel(Material.STICK, 5);
        mapEntity.getEntityMeta().setScale(new Vec(4));
        mapEntity.setInstance(hub.instance(), MAP_ENTITY_POS).join();
        hub.scheduler().submitTask(this::mapEntityUpdate, ExecutionType.SYNC);
    }

    private @NotNull TaskSchedule mapEntityUpdate() {
        var meta = mapEntity.getEntityMeta();
        meta.setNotifyAboutChanges(false);

        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(20 * MAP_ENTITY_UPDATE_INTERVAL);
        meta.setLeftRotation(new Quaternion(new Vec(0, 0, 1).normalize(), Math.toRadians(mapEntityRotationTarget)).into());
        mapEntityRotationTarget += 90;

        meta.setNotifyAboutChanges(true);
        return TaskSchedule.seconds(MAP_ENTITY_UPDATE_INTERVAL);
    }

}
