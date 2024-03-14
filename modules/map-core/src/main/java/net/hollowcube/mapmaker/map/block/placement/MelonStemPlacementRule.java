package net.hollowcube.mapmaker.map.block.placement;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class MelonStemPlacementRule extends BaseBlockPlacementRule {
    private static final BlockFace[] HORIZONTAL_FACES = new BlockFace[]{
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    };

    public MelonStemPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull BlockPlacementRule.PlacementState placement) {
        return block;
    }

    @Override
    public @NotNull Block blockUpdate(@NotNull BlockPlacementRule.UpdateState update) {
        var instance = update.instance();
        var block = update.currentBlock();
        var fromBlock = instance.getBlock(update.blockPosition().relative(update.fromFace()));

        if (block.id() == Block.MELON_STEM.id()) {
            // Attach the stem to the melon if it's fully grown, otherwise do nothing.
            if (!isFullyGrown(block) || fromBlock.id() != Block.MELON.id()) return block;
            return Block.ATTACHED_MELON_STEM.withProperty("facing", update.fromFace().name().toLowerCase(Locale.ROOT));
        }

        // At this point we are dealing with an attached stem.
        // If the update comes from a different direction than the stem is currently facing, do nothing.
        var stemFacing = BlockFace.valueOf(block.getProperty("facing").toUpperCase(Locale.ROOT));
        if (update.fromFace() != stemFacing || fromBlock.id() == Block.MELON.id()) return block;

        // If we reached here then we got an update from the current facing direction and it is not a melon, so we should
        // first try to find an adjacent melon and connect to it, otherwise switch back to a fully grown non-attached stem block.

        // Note: The above behavior is disabled, it just disconnects the stem. This seems like kinda unintuitive behavior, however this is what vanilla predicts.
        // Leaving this commented out in case we want to revert to this behavior.
//        for (var face : HORIZONTAL_FACES) {
//            var adjacentBlock = instance.getBlock(update.blockPosition().relative(face));
//            if (adjacentBlock.id() == Block.MELON.id()) {
//                return Block.ATTACHED_MELON_STEM.withProperty("facing", face.name().toLowerCase(Locale.ROOT));
//            }
//        }

        return Block.MELON_STEM.withProperty("age", "7");
    }

    @Override
    public int maxUpdateDistance() {
        return 1;
    }

    private boolean isFullyGrown(@NotNull Block block) {
        var age = Integer.parseInt(block.getProperty("age"));
        return age == 7;
    }
}
