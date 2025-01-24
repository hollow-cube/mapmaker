package net.hollowcube.mapmaker.map.block.placement;


import net.hollowcube.mapmaker.map.block.handler.BannerBlockHandler;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

            return withBannerData(block, placementState.usedItemStack())
                    .withProperty("rotation", String.valueOf(rotation));
        }

        return withBannerData(toWallBlock(block), placementState.usedItemStack())
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

    private Block withBannerData(Block block, @Nullable ItemStack stack) {
        return block.withTag(BannerBlockHandler.PATTERNS, stack == null ? null : stack.get(ItemComponent.BANNER_PATTERNS));
    }

}
