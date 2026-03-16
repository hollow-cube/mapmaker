package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChestPlacementRule extends WaterloggedPlacementRule {

    private static final BlockFace[][] CONNECTION_FACES = new BlockFace[][]{
        // Indices are BlockFace#ordinal() - 2
        /*North*/{BlockFace.EAST, BlockFace.WEST},
        /*South*/{BlockFace.WEST, BlockFace.EAST},
        /*West*/{BlockFace.NORTH, BlockFace.SOUTH},
        /*East*/{BlockFace.SOUTH, BlockFace.NORTH}
    };

    public ChestPlacementRule(Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(BlockPlacementRule.PlacementState placement) {
        final Point blockPosition = placement.placePosition();
        final Pos playerPosition = Objects.requireNonNullElse(placement.playerPosition(), Pos.ZERO);
        BlockFace facing = BlockFace.fromYaw(playerPosition.yaw()).getOppositeFace();

        String type = "single";
        if (!placement.isPlayerShifting()) {
            for (int i = 0; i < 2; i++) {
                final BlockFace connection = CONNECTION_FACES[facing.ordinal() - 2][i];
                final Block neighbor = placement.instance().getBlock(blockPosition.relative(connection), Block.Getter.Condition.TYPE);

                // Must be the same type chest (ie chest or trapped chest)
                if (neighbor.id() != this.block.id()) continue;
                // Must be facing the same way
                if (!neighbor.getProperty("facing").equals(facing.toString().toLowerCase(Locale.ROOT))) continue;
                // Must be a single chest
                if (!neighbor.getProperty("type").equals("single")) continue;

                type = i == 0 ? "left" : "right";
                break;
            }
        } else if (placement.blockFace() != null) {
            Point pos = blockPosition.relative(placement.blockFace().getOppositeFace());
            Block neighbor = placement.instance().getBlock(pos, Block.Getter.Condition.TYPE);

            if (neighbor.id() == this.block.id() && neighbor.getProperty("type").equals("single")) {
                BlockFace neighborFacing = BlockFace.valueOf(neighbor.getProperty("facing").toUpperCase(Locale.ROOT));

                if (neighborFacing.isSimilar(facing)) {
                    facing = neighborFacing;

                    if (CONNECTION_FACES[neighborFacing.ordinal() - 2][0] == placement.blockFace()) {
                        type = "right";
                    } else {
                        type = "left";
                    }
                }
            }
        }

        return block.withProperties(Map.of(
                "type", type,
                "facing", facing.toString().toLowerCase(Locale.ROOT),
                "waterlogged", waterlogged(placement)
        ));
    }

    @Override
    public Block blockUpdate(BlockPlacementRule.UpdateState update) {
        final Block currentBlock = update.currentBlock();
        final Point blockPosition = update.blockPosition();

        final Block fromBlock = update.instance().getBlock(blockPosition.relative(update.fromFace()), Block.Getter.Condition.TYPE);
        if (fromBlock.isAir()) {
            // Block was destroyed, so we may want to revert to a single chest
            final BlockFace facing = BlockFace.valueOf(currentBlock.getProperty("facing").toUpperCase(Locale.ROOT));
            final String type = currentBlock.getProperty("type");
            // Single chests never need break updates
            if ("single".equals(type)) return super.blockUpdate(update);

            BlockFace expectedFace = CONNECTION_FACES[facing.ordinal() - 2][type.equals("left") ? 0 : 1];
            if (expectedFace != update.fromFace()) return super.blockUpdate(update);

            return currentBlock.withProperty("type", "single");
        }

        // We only care about updates from chests.
        if (fromBlock.id() != this.block.id()) return super.blockUpdate(update);

        // The updating chest must be facing our direction
        final BlockFace fromFacing = BlockFace.valueOf(fromBlock.getProperty("facing").toUpperCase(Locale.ROOT));
        if (!fromBlock.getProperty("facing").equals(update.currentBlock().getProperty("facing")))
            return super.blockUpdate(update);

        // The updating chest must be type=towards us.
        String fromType = fromBlock.getProperty("type");
        if ("single".equals(fromType)) return super.blockUpdate(update);
        int targetIndex = "right".equals(fromType) ? 0 : 1;
        BlockFace targetFace = CONNECTION_FACES[fromFacing.ordinal() - 2][targetIndex];
        if (targetFace != update.fromFace()) return super.blockUpdate(update);

        // Yay we can connect to it
        return update.currentBlock().withProperty("type", targetIndex == 0 ? "left" : "right");
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

}
