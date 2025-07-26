package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.thread.TickThread;
import net.minestom.server.utils.PacketSendingUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface PressurePlateBlockMixin extends BlockHandler {
    BoundingBox BOUNDING_BOX = new BoundingBox(14.0 / 16.0, 4.0 / 16.0, 14.0 / 16.0);

    @Override
    default boolean isTickable() {
        return true;
    }

    @Override
    default void tick(@NotNull Tick tick) {
        var instance = tick.getInstance();

        // Particles trigger in editing worlds, so handle this first.
        particleTick(instance, tick.getBlockPosition());
    }

    default void particleTick(@NotNull Instance instance, @NotNull Point blockPosition) {

        // Spawn particles in the world every once in a while if building
        //noinspection UnstableApiUsage,DataFlowIssue
        if (TickThread.current().getTick() % 20 != 0) return;
        var world = MapWorld.unsafeFromInstance(instance);
        if (!(world instanceof EditingMapWorld)) return;

        PacketSendingUtils.sendGroupedPacket(world.players(), new ParticlePacket(
                Particle.DUST.withProperties(new Color(0, 255, 0), 1f),
                false, true, blockPosition.x() + 0.5, blockPosition.y() + 0.5, blockPosition.z() + 0.5,
                0.25f, 0.25f, 0.25f, 0, 5
        ));
    }

    void onPlatePressed(@NotNull Tick tick, @NotNull Player player);

    default void onPlateReleased(@NotNull Tick tick, @NotNull Player player) {
        // Default implementation does nothing
    }

    /**
     * MUST return a mutable set used for internal statekeeping.
     */
    @Deprecated
    @NotNull
    Set<Player> getPlayersOnPlate();
}
