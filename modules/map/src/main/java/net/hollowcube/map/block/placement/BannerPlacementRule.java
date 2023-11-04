package net.hollowcube.map.block.placement;


import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// Initially taken from https://github.com/Minestom/Minestom/pull/1759
public class BannerPlacementRule extends BaseBlockPlacementRule {

    public BannerPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public Block blockPlace(@NotNull PlacementState placementState) {
        // Can't place at the bottom of a block
        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP); // Top is an arbitrary choice.
        if (blockFace == BlockFace.BOTTOM) {
            return null;
        }

        if (blockFace == BlockFace.TOP) {
            var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
            float yaw = playerPosition.yaw() + 180;
            int rotation = (int) (Math.round(yaw / 22.5d) % 16);

            return withBannerData(block)
                    .withProperty("rotation", String.valueOf(rotation));
        }

        return withBannerData(toWallBlock(block))
                .withProperty("facing", blockFace.name().toLowerCase());
    }

    private Block toWallBlock(Block block) {
        // Same as skulls, maybe there's a better way
        String name = block.namespace().value();

        // white_banner -> white
        String rawName = name.substring(0, name.lastIndexOf("_"));

        return Block.fromNamespaceId(rawName + "_wall_banner")
                .withHandler(block.handler());
    }

    private Block withBannerData(Block block/*, BannerMeta meta*/) {
        // TODO missing banner meta, waiting for https://github.com/Minestom/Minestom/pull/1274
        //  Also missing ItemMeta from placeBlock() in https://github.com/Minestom/Minestom/pull/1758
        return block;
    }

}
