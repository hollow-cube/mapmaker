package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNode;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNodeData;
import net.kyori.adventure.key.Key;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.PacketSendingUtils;

public final class PressurePlateBlock implements
    BlockHandler,
    CollidableBlock,
    DebugCommand.BlockDebug {

    private static final BoundingBox COLLISION_BOX = new BoundingBox(
        14.0 / 16.0,
        4.0 / 16.0,
        14.0 / 16.0
    );
    private static final Particle DUST_PARTICLE = Particle.DUST.withProperties(
        new Color(0, 255, 0),
        1f
    );

    public static final Key KEY = Key.key("mapmaker", "pressure_plate");
    public static final PressurePlateBlock INSTANCE = new PressurePlateBlock();

    // the old id we used for specific plates. it was part of the ID of the plate and we must maintain that.
    public static final Tag<String> LEGACY_ID_TAG = Tag.String("legacy_id");

    private PressurePlateBlock() {}

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public BoundingBox collisionBox() {
        return COLLISION_BOX;
    }

    @Override
    public Trigger createTrigger(BlockVec position, Block block) {
        var prefix = block.hasTag(LEGACY_ID_TAG) ? block.getTag(LEGACY_ID_TAG) : KEY.asString();
        var id = String.format(
            "%s/%d_%d_%d",
            prefix,
            position.blockX(),
            position.blockY(),
            position.blockZ()
        );
        return new TriggerNode(id, position, block.getTag(TriggerNodeData.TAG));
    }

    @Override
    public void sendDebugInfo(Player player, Block block) {
        player.sendMessage(
            NbtUtil.prettyPrint(TriggerNodeData.CODEC, block.getTag(TriggerNodeData.TAG))
        );
    }

    @Override
    public boolean isTickable() {
        return true;
    }

    @Override
    public void tick(Tick tick) {
        // Spawn particles in the world every once in a while if building
        final var instance = tick.getInstance();
        if (instance.getWorldAge() % 20 != hashCode() % 20) return;

        var world = MapWorld.forInstance(instance);
        if (world == null || !world.canEdit(null)) return;

        final var blockPosition = tick.getBlockPosition();
        PacketSendingUtils.sendGroupedPacket(
            world.players(),
            new ParticlePacket(
                DUST_PARTICLE,
                false,
                true,
                blockPosition.x() + 0.5,
                blockPosition.y() + 0.5,
                blockPosition.z() + 0.5,
                0.25f,
                0.25f,
                0.25f,
                0,
                5
            )
        );
    }
}
