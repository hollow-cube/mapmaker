package net.hollowcube.mapmaker.hub.feature.motw;

import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(HubFeature.class)
public class MapOfTheWeekFeature implements HubFeature {
    private static final Logger logger = LoggerFactory.getLogger(MapOfTheWeekFeature.class);

    private static final Pos MAP_ENTITY_POS = new Pos(-38 + 0.5, 43, 54 + 0.5, 0, -90);
    private static final int MAP_ENTITY_UPDATE_INTERVAL = 5; // Seconds

    private ServerBridge bridge;

    private final NpcItemModel mapEntity = new NpcItemModel();
    private int mapEntityRotationTarget = 0;

    @Inject
    public MapOfTheWeekFeature(@NotNull ServerBridge bridge, @NotNull HubMapWorld world, @NotNull Scheduler scheduler) {
        this.bridge = bridge;

        // Timer init (the big countdown above the map)
        var timer = new CountdownTimer(world.instance());
        scheduler.submitTask(timer, ExecutionType.SYNC);

        // Spinning map
        mapEntity.setHandler(this::handleMapInteract);
        mapEntity.setModel(Material.STICK, 5);
        mapEntity.getEntityMeta().setScale(new Vec(4));
        mapEntity.setInstance(world.instance(), MAP_ENTITY_POS).join();
        mapEntity.setInteractionBox(6, 6, new Pos(0, -0.5, 0)).join();
        scheduler.submitTask(this::mapEntityUpdate, ExecutionType.SYNC);
    }

    private void handleMapInteract(@NotNull Player player, @NotNull BaseNpcEntity npc, Player.@NotNull Hand hand) {
        try {
            player.sendMessage(Component.translatable("motw.joining"));
            bridge.joinMap(player, "14b8a361-7cba-49ec-933c-14faad11f385", ServerBridge.JoinMapState.PLAYING);
        } catch (Exception e) {
            // If an error occurs here the player is still here, it is our responsibility to handle this (with an error)
            logger.error("failed to join motw for {}: {}", PlayerDataV2.fromPlayer(player).id(), e.getMessage());
            player.sendMessage(Component.translatable("command.generic.unknown_error"));
        }
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
