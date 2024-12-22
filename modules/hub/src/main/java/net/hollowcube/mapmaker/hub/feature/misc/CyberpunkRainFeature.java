package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.common.physics.BoundingBox;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ChangeGameStatePacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.function.Predicate.not;

@AutoService(HubFeature.class)
public class CyberpunkRainFeature implements HubFeature {
    private static final Tag<Float> RAIN_LEVEL = Tag.Float("hub/rain_level").defaultValue(0.0f);

    private static final Point MIN = new Vec(-184, 60, -98);
    private static final Point MAX = new Vec(-105, 128, -31);
    private static final Point SIZE = MAX.sub(MIN);
    private static final BoundingBox BOUNDING_BOX = new BoundingBox(SIZE.x(), SIZE.y(), SIZE.z(), MIN);

    private static final List<Point> LIGHTNING_POINTS = List.of(
            new Vec(-128, 83, -78),
            new Vec(-157, 81, -63),
            new Vec(-169, 83, -49),
            new Vec(-170, 63, -31),
            new Vec(-173, 86, -84),
            new Vec(-156, 114, -74),
            new Vec(-109, 94, -63)
    );

    private Instance instance;
    private final Set<Player> rainyPlayers = new HashSet<>();

    private Entity lightningEntity = null;

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        this.instance = world.instance();

        server.scheduler().submitTask(this::updateCollision);
        server.scheduler().submitTask(this::lightningTask);
    }

    private @NotNull TaskSchedule lightningTask() {
        var point = LIGHTNING_POINTS.get(ThreadLocalRandom.current().nextInt(LIGHTNING_POINTS.size()));

        if (lightningEntity != null) lightningEntity.remove();
        lightningEntity = new Entity(EntityType.LIGHTNING_BOLT) {
            @Override protected void movementTick() {
                // Intentionally do nothing
            }
        };
        lightningEntity.setAutoViewable(false);
        lightningEntity.setInstance(instance, point);
        rainyPlayers.forEach(lightningEntity::addViewer);

//        var nextLightingTicks = ThreadLocalRandom.current().nextInt(20 * 60) + (20 * 60);
//        return TaskSchedule.tick(nextLightingTicks);
        return TaskSchedule.tick(20 * 15);
    }

    private @NotNull TaskSchedule updateCollision() {
        var instancePlayers = instance.getPlayers();
        rainyPlayers.removeIf(not(instancePlayers::contains));
        for (var player : instancePlayers) {
            var isInRain = BOUNDING_BOX.intersectBox(player.getPosition().mul(-1), player.getBoundingBox());
            if (isInRain && !rainyPlayers.contains(player)) {
                rainyPlayers.add(player);
                onEnterRain(player);
            }
            if (!isInRain && rainyPlayers.contains(player)) {
                rainyPlayers.remove(player);
                onExitRain(player);
            }

            onRainTick(player, isInRain);
        }

        return TaskSchedule.tick(2);
    }

    private void onEnterRain(@NotNull Player player) {
        player.sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.BEGIN_RAINING, 0));
    }

    private void onExitRain(@NotNull Player player) {
    }

    private void onRainTick(@NotNull Player player, boolean isInRain) {
        if (!player.hasTag(RAIN_LEVEL) && !isInRain) return;

        var rainLevel = player.getTag(RAIN_LEVEL);
        if (isInRain && rainLevel >= 1.0f) return; // Already high enough
        if (!isInRain && rainLevel <= 0) {
            player.sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.END_RAINING, 0));
            player.removeTag(RAIN_LEVEL);
            return;
        }

        rainLevel += isInRain ? 0.04f : -0.06f;
        player.setTag(RAIN_LEVEL, rainLevel);
        player.sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.RAIN_LEVEL_CHANGE, rainLevel));
        player.sendPacket(new ChangeGameStatePacket(ChangeGameStatePacket.Reason.THUNDER_LEVEL_CHANGE, rainLevel));
    }

}
