package net.hollowcube.map.block.interaction;

import net.hollowcube.map.entity.impl.ItemFrameEntity;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.metadata.other.ItemFrameMeta;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ItemFrameInteractionRule implements BlockInteractionRule {

    private final boolean glowing;

    public ItemFrameInteractionRule(boolean glowing) {
        this.glowing = glowing;
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var entity = this.glowing ? new ItemFrameEntity.Glowing(UUID.randomUUID()) : new ItemFrameEntity(UUID.randomUUID());

        var meta = (ItemFrameMeta) entity.getEntityMeta();
        meta.setOrientation(switch (interaction.blockFace()) {
            case BOTTOM -> ItemFrameMeta.Orientation.DOWN;
            case TOP -> ItemFrameMeta.Orientation.UP;
            case NORTH -> ItemFrameMeta.Orientation.NORTH;
            case SOUTH -> ItemFrameMeta.Orientation.SOUTH;
            case WEST -> ItemFrameMeta.Orientation.WEST;
            case EAST -> ItemFrameMeta.Orientation.EAST;
        });

        var pos = this.calculatePlacementPos(interaction.blockPosition(), interaction.blockFace());
        entity.setInstance(interaction.instance(), pos);
        entity.playSpawnSound();

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

        // The yaw and pitch are used to rotate the item frame to face the correct direction. These are always
        // snapped to whole values to ensure the item frame is snapped to the block.
        // This logic is mostly taken from vanilla
        float yaw;
        float pitch;
        if (isHorizontal(direction)) {
            yaw = getDegreesForHorizontalDirection(direction);
            pitch = 0F;
        } else {
            yaw = 0F;
            pitch = direction == Direction.UP ? -90F : 90F; // Up is -90, down is 90
        }

        return new Pos(x, y, z, yaw, pitch);
    }

    private static boolean isHorizontal(@NotNull Direction direction) {
        return direction != Direction.UP && direction != Direction.DOWN;
    }

    private static float getDegreesForHorizontalDirection(@NotNull Direction direction) {
        // Minecraft horizontal directions go anticlockwise from south
        return switch (direction) {
            case SOUTH -> 0;
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> throw new IllegalArgumentException("Direction must be horizontal");
        };
    }
}
