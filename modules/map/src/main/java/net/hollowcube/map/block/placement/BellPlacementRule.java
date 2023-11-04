package net.hollowcube.map.block.placement;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

//todo this needs to be updated based on the changes to placement
public class BellPlacementRule extends BaseBlockPlacementRule {
    private static final String FACING = "facing";
    private static final String ATTACHMENT = "attachment";

    public BellPlacementRule() {
        super(Block.BELL);
    }

    @Override
    public @NotNull Block blockPlace(@NotNull PlacementState placementState) {
        Block.Getter instance = placementState.instance();
        Block block = placementState.block();
        int x = placementState.placePosition().blockX();
        int y = placementState.placePosition().blockY();
        int z = placementState.placePosition().blockZ();

        BlockFace clickedFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
        Direction clickedDirection = clickedFace.toDirection();
        Direction facing = BlockFace.fromYaw(playerPosition.yaw()).toDirection();

        if (clickedFace == BlockFace.BOTTOM) {
            // If we clicked on the bottom of a block, the bell is hanging from the ceiling
            return block.withProperty(FACING, facing.name().toLowerCase(Locale.ROOT)).withProperty(ATTACHMENT, "ceiling");
        }
        if (clickedFace == BlockFace.TOP) {
            // If we clicked on the top of a block, the bell is on the floor
            return block.withProperty(FACING, facing.name().toLowerCase(Locale.ROOT)).withProperty(ATTACHMENT, "floor");
        }

        Direction oppositeDirection = clickedDirection.opposite();
        block = block.withProperty(FACING, oppositeDirection.name().toLowerCase(Locale.ROOT));

        // The weird thing here is that the offset is subtracted rather than added.
        // The reason why this is done is that the clicked direction is actually going to be the direction that the face we clicked on is facing,
        // not the direction that we clicked on the face from, and we want the opposite of that.
        Block placedOn = instance.getBlock(x - clickedDirection.normalX(), y, z - clickedDirection.normalZ());

        // If the block we placed on is a bell, don't attach to it. This is a vanilla thing.
        if (placedOn.compare(Block.BELL)) return block.withProperty(ATTACHMENT, "floor");

        Block oppositePlacedOn = instance.getBlock(x - oppositeDirection.normalX(), y, z - oppositeDirection.normalZ());
        // If the block opposite is air then we know there's only a single attachment, and so it's just single, otherwise there's two
        // attachments and it's double.
        // We don't need to check if the block we placed on is air because you can't place a block on air.
        String attachment = isAir(oppositePlacedOn) ? "single_wall" : "double_wall";
        return block.withProperty(ATTACHMENT, attachment);
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull UpdateState updateState) {
        Block.Getter instance = updateState.instance();
        Block block = updateState.currentBlock();
        int x = updateState.blockPosition().blockX();
        int y = updateState.blockPosition().blockY();
        int z = updateState.blockPosition().blockZ();
        Direction facing = Direction.valueOf(block.getProperty(FACING).toUpperCase(Locale.ROOT));

        String attachment = block.getProperty(ATTACHMENT);
        if (attachment == null) return updateState.currentBlock();
        // The attachment doesn't change if you place another block around a bell attached to the ceiling or floor
        if (attachment.equals("ceiling") || attachment.equals("floor")) return updateState.currentBlock();

        Direction opposite = facing.opposite();
        Block placedOn = instance.getBlock(x + facing.normalX(), y, z + facing.normalZ());
        Block oppositePlacedOn = instance.getBlock(x + opposite.normalX(), y, z + opposite.normalZ());

        // This isn't vanilla behaviour, as vanilla will pop the item and drop it if you remove the block it's attached to.
        // However, this behaviour was requested by matt, who thought it would look better if the bell had a double attachment when it is
        // not attached to anything, but was placed on a wall.
        // We check this early because it means that no matter what happens, the bell will always be set to double if it is no longer attached
        // to any blocks.
        if (isAir(placedOn) && isAir(oppositePlacedOn)) return block.withProperty(ATTACHMENT, "double_wall");

        // If the bell was attached to a single block (single_wall), and it now has two blocks on opposite sides of each other, the
        // attachment is changed to double_wall.
        if (attachment.equals("single_wall") && !isAir(placedOn) && !isAir(oppositePlacedOn)) {
            return block.withProperty(ATTACHMENT, "double_wall");
        }

        if (attachment.equals("double_wall")) {
            // We check these individually because you can only change one block at a time (in a way that triggers blockUpdate), so only
            // one of the placed on or opposite placed on could have changed.

            // If the placed on block is now air (if it's double_wall, it wasn't before), then we reorient the bell to face the opposite
            // direction and connect to the opposite block instead.
            if (isAir(placedOn)) {
                return block.withProperty(FACING, opposite.name().toLowerCase(Locale.ROOT)).withProperty(ATTACHMENT, "single_wall");
            }
            // If the opposite placed on block is now air, we change the attachment to single_wall. We don't need to reorient the bell,
            // as it is already facing the direction that we want.
            if (isAir(oppositePlacedOn)) {
                return block.withProperty(ATTACHMENT, "single_wall");
            }
        }

        return block;
    }

    private static boolean isAir(@NotNull Block block) {
        return block.compare(Block.AIR) || block.compare(Block.VOID_AIR) || block.compare(Block.CAVE_AIR);
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }
}
