package net.hollowcube.map.block.handler;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.worldold.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.particle.Particle;
import net.minestom.server.particle.ParticleCreator;
import net.minestom.server.thread.TickThread;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public interface PressurePlateBlockMixin extends BlockHandler {
    BoundingBox BOUNDING_BOX = new BoundingBox(14.0 / 16.0, 1.0 / 16.0, 14.0 / 16.0);

    @Override
    default boolean isTickable() {
        return true;
    }

    @Override
    default void tick(@NotNull Tick tick) {
        var instance = tick.getInstance();
        var pos = tick.getBlockPosition();
        var centerPos = new Vec(pos.blockX() + 0.5, pos.blockY(), pos.blockZ() + 0.5);

        particleTick(instance, tick.getBlockPosition());

        Set<Player> newPlayers = new HashSet<>(), currentPlayers = getPlayersOnPlate();

        // Check for collision with all players in instance
        var entities = instance.getNearbyEntities(pos, 2);
        if (entities.isEmpty()) {
            currentPlayers.clear();
            return;
        }
        for (var entity : entities) {
            Player player;
            if (entity instanceof Player p) {
                player = p;
            } else if (entity.hasTag(MapHooks.ASSOCIATED_PLAYER)) {
                player = entity.getTag(MapHooks.ASSOCIATED_PLAYER);
            } else continue;

            // To trigger the plate they must be
            // 1: in the playing state
            // 2: if this is not a testing state they must have have a start time (ie has moved/started the timer)
            // 3: be in the bounding box
            if (!MapHooks.isPlayerPlaying(player)) continue;
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState == null || (saveState.getPlayStartTime() == 0)) continue;
            if (!BOUNDING_BOX.intersectBox(centerPos.sub(entity.getPosition()), entity.getBoundingBox()))
                continue;

            // Player is on the plate
            newPlayers.add(player);
        }

        // Diff the new players with the old players
        for (var player : newPlayers) {
            if (!currentPlayers.contains(player)) {
                onPlatePressed(tick, player);
            }
        }
        currentPlayers.clear();
        currentPlayers.addAll(newPlayers);
    }

    default void particleTick(@NotNull Instance instance, @NotNull Point blockPosition) {

        // Spawn particles in the world every once in a while if building
        //noinspection UnstableApiUsage,DataFlowIssue
        if (TickThread.current().getTick() % 20 != 0) return;
        var world = MapWorld.unsafeFromInstance(instance);
        if (world == null || ((world.flags()) & MapWorld.FLAG_EDITING) == 0) return;

        PacketUtils.sendGroupedPacket(world.players(), ParticleCreator.createParticlePacket(
                Particle.DUST, true,
                blockPosition.x() + 0.5, blockPosition.y() + 0.5, blockPosition.z() + 0.5,
                0.25f, 0.25f, 0.25f, 0f, 5, buffer -> {
                    buffer.writeFloat(0f); // red
                    buffer.writeFloat(1f); // green
                    buffer.writeFloat(0f); // blue
                    buffer.writeFloat(1f); // scale
                }
        ));
    }

    void onPlatePressed(@NotNull Tick tick, @NotNull Player player);

    /**
     * MUST return a mutable set used for internal statekeeping.
     */
    @ApiStatus.Internal
    @NotNull Set<Player> getPlayersOnPlate();
}
