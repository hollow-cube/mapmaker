package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.color.Color;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.utils.PacketSendingUtils;

public interface PressurePlateBlock extends CollidableBlock {
    BoundingBox COLLISION_BOX = new BoundingBox(14.0 / 16.0, 4.0 / 16.0, 14.0 / 16.0);

    Particle DUST_PARTICLE = Particle.DUST.withProperties(new Color(0, 255, 0), 1f);

    @Override
    default BoundingBox collisionBox() {
        return COLLISION_BOX;
    }

    @Override
    default boolean isTickable() {
        return true;
    }

    @Override
    default void tick(Tick tick) {
        // Spawn particles in the world every once in a while if building
        final var instance = tick.getInstance();
        if (instance.getWorldAge() % 20 != hashCode() % 20) return;

        var world = MapWorld.forInstance(instance);
        if (world == null || !world.canEdit(null)) return;

        final var blockPosition = tick.getBlockPosition();
        PacketSendingUtils.sendGroupedPacket(world.players(), new ParticlePacket(
                DUST_PARTICLE, false, true,
                blockPosition.x() + 0.5, blockPosition.y() + 0.5, blockPosition.z() + 0.5,
                0.25f, 0.25f, 0.25f, 0, 5
        ));
    }

}
