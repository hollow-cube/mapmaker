package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.entity.impl.physics.SulfurCubeEntity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.cube.SulfurCubeArchetype;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@AutoService(HubFeature.class)
public class SulfurCubeBallFeature implements HubFeature {
    private static final Pos SPAWN = new Pos(-86.5, 48, 0.5);
    private static final SulfurCubeArchetype ARCHETYPE = MinecraftServer.process()
        .sulfurCubeArchetype().get(SulfurCubeArchetype.BOUNCY);
    private static final ItemStack BODY_ITEM = ItemStack.of(Material.OAK_WOOD);

    // hub has a really high entity interaction range, so range-check hits ourselves.
    private static final double MAX_HIT_DISTANCE = 5;
    private static final float HIT_DAMAGE = 3f; // 3x a bare-hand hit

    private static final double NEARBY_RADIUS = 16;
    private static final int CHECK_INTERVAL_SECONDS = 5;
    private static final int EMPTY_RESPAWN_SECONDS = 30;

    private HubMapWorld world;
    private SulfurCubeEntity cube;
    private int emptySeconds = 0;

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        this.world = world;
        respawn();

        world.eventNode().addListener(EntityAttackEvent.class, this::handleHit);
        server.scheduler().submitTask(this::boundsTick, ExecutionType.TICK_START);
        server.scheduler().submitTask(this::presenceTick, ExecutionType.TICK_START);
    }

    private void handleHit(@NotNull EntityAttackEvent event) {
        if (event.getTarget() != cube || !(event.getEntity() instanceof Player player)) return;

        var eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        if (eye.distance(cube.getPosition()) > MAX_HIT_DISTANCE) return;

        cube.applyHitKnockback(player.getPosition(), eye, player.getPosition().direction(), HIT_DAMAGE);
    }

    private @NotNull TaskSchedule boundsTick() {
        if (cube.isRemoved() || !HubMapWorld.inWorldBounds(cube.getPosition())) respawn();
        return TaskSchedule.nextTick();
    }

    private @NotNull TaskSchedule presenceTick() {
        if (playersNearby()) {
            emptySeconds = 0;
        } else {
            emptySeconds += CHECK_INTERVAL_SECONDS;
            if (emptySeconds >= EMPTY_RESPAWN_SECONDS) respawn();
        }
        return TaskSchedule.seconds(CHECK_INTERVAL_SECONDS);
    }

    private boolean playersNearby() {
        var origin = cube.getPosition();
        for (var player : world.instance().getPlayers()) {
            if (player.getPosition().distanceSquared(origin) <= NEARBY_RADIUS * NEARBY_RADIUS) return true;
        }
        return false;
    }

    private void respawn() {
        if (cube != null) cube.remove();

        cube = new SulfurCubeEntity(UUID.randomUUID());
        cube.setBodyBlock(BODY_ITEM);
        cube.setArchetype(ARCHETYPE);
        cube.setInstance(world.instance(), SPAWN);
        emptySeconds = 0;
    }
}
