package net.hollowcube.mapmaker.hub.feature.motw;

import com.google.auto.service.AutoService;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.feature.contest.MapContest;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.Material;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

//@AutoService(HubFeature.class)
public class MapOfTheWeekFeature implements HubFeature {
    private static final Logger logger = LoggerFactory.getLogger(MapOfTheWeekFeature.class);

    private static final Pos MAP_ENTITY_POS = new Pos(-38 + 0.5, 43, 54 + 0.5, 0, -90);
    private static final int MAP_ENTITY_UPDATE_INTERVAL = 5; // Seconds

    private ServerBridge bridge;

    private final NpcItemModel mapEntity = new NpcItemModel();
    private int mapEntityRotationTarget = 0;

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        this.bridge = server.bridge();

        // Timer init (the big countdown above the map)
        var timer = new CountdownTimer(world.instance());
        server.scheduler().submitTask(timer, ExecutionType.TICK_START);

        // Spinning map
        mapEntity.setHandler(this::handleMapInteract);
        mapEntity.setModel(Material.STICK, BadSprite.require("sm_house"));
        mapEntity.getEntityMeta().setScale(new Vec(4));
        mapEntity.setInstance(world.instance(), MAP_ENTITY_POS);
        mapEntity.setInteractionBox(6, 6, new Pos(0, -0.5, 0));
        MapContest.scheduleAtStart(world, () -> {
            mapEntity.setModel(Material.STICK, BadSprite.require("item_comp_10"));
            mapEntity.getEntityMeta().setScale(new Vec(5));
            mapEntity.teleport(MAP_ENTITY_POS.add(0, 2.5, 0).withView(0, 0));
        });

        server.scheduler().submitTask(this::mapEntityUpdate, ExecutionType.TICK_START);
    }

    private void handleMapInteract(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull PlayerHand hand, boolean isLeftClick) {
        if (isLeftClick) return;
        var millisToStart = ChronoUnit.MILLIS.between(LocalDateTime.now(), MapContest.START_DATE);
        if (millisToStart > 0) {
            player.sendMessage(Component.translatable("motw.coming_soon"));
            return;
        }

        MapContest.openSubmissionMenu(player);
    }

    private @NotNull TaskSchedule mapEntityUpdate() {
        var meta = mapEntity.getEntityMeta();
        meta.setNotifyAboutChanges(false);

        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(20 * MAP_ENTITY_UPDATE_INTERVAL);
        var millisToStart = ChronoUnit.MILLIS.between(LocalDateTime.now(), MapContest.START_DATE);
        var rot = millisToStart > 0 ? new Vec(0, 0, 1) : new Vec(0, 1, 0);
        meta.setLeftRotation(new Quaternion(rot.normalize(), Math.toRadians(mapEntityRotationTarget)).into());
        mapEntityRotationTarget += 90;

        meta.setNotifyAboutChanges(true);
        return TaskSchedule.seconds(MAP_ENTITY_UPDATE_INTERVAL);
    }

}
