package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.entity.impl.other.PaintingEntity;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.metadata.other.PaintingMeta;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.hollowcube.mapmaker.map.block.interaction.ItemFrameInteractionRule.getDegreesForHorizontalDirection;

public class PaintingInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockFace = interaction.blockFace();
        if (blockFace == BlockFace.TOP || blockFace == BlockFace.BOTTOM) return false;

        var entity = new PaintingEntity(UUID.randomUUID());

        var variant = interaction.item().get(DataComponents.PAINTING_VARIANT);
        if (variant != null) entity.set(DataComponents.PAINTING_VARIANT, variant);

        var meta = entity.getEntityMeta();
        meta.setOrientation(switch (blockFace) {
            case NORTH -> PaintingMeta.Orientation.NORTH;
            case SOUTH -> PaintingMeta.Orientation.SOUTH;
            case WEST -> PaintingMeta.Orientation.WEST;
            case EAST -> PaintingMeta.Orientation.EAST;
            case TOP, BOTTOM -> throw new IllegalStateException("unreachable");
        });

        var pos = this.calculatePlacementPos(interaction.blockPosition(), interaction.blockFace());
        entity.setInstance(interaction.instance(), pos);
        entity.playSpawnSound();

        interaction.player().swingMainHand();

        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }

    private @NotNull Pos calculatePlacementPos(@NotNull Point blockPosition, @NotNull BlockFace face) {
        Direction direction = face.toDirection();

        // We offset the block position relative to the face direction so that we place the frame on the block
        // in front of the block we clicked, not inside of it
        double x = blockPosition.x() + direction.normalX();
        double y = blockPosition.y() + direction.normalY();
        double z = blockPosition.z() + direction.normalZ();

        var yaw = getDegreesForHorizontalDirection(direction);
        return new Pos(x, y, z, yaw, 0);
    }

}
