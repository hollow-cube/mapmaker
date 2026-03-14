package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.common.physics.BoundingBox;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Weather;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.function.Predicate.not;

//@AutoService(HubFeature.class)
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

    private @UnknownNullability Instance instance; // lateinit
    private final Set<Player> rainyPlayers = new HashSet<>();

    private @Nullable Entity lightningEntity = null;

    @Override
    public void load(MapServer server, HubMapWorld world) {
        this.instance = world.instance();

        server.scheduler().submitTask(this::updateCollision);
        server.scheduler().submitTask(this::lightningTask);
    }

    private TaskSchedule lightningTask() {
        var point = LIGHTNING_POINTS.get(ThreadLocalRandom.current().nextInt(LIGHTNING_POINTS.size()));

        if (lightningEntity != null) lightningEntity.remove();
        lightningEntity = new Entity(EntityType.LIGHTNING_BOLT) {
            @Override
            protected void movementTick() {
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

    private TaskSchedule updateCollision() {
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
        }

        return TaskSchedule.tick(2);
    }

    private void onEnterRain(Player player) {
        if (!(player instanceof MapPlayer mp)) return;
        mp.setWeather(Weather.THUNDER, 0.02f);
    }

    private void onExitRain(Player player) {
        if (!(player instanceof MapPlayer mp)) return;
        mp.setWeather(Weather.CLEAR, 0.03f);
    }

}
