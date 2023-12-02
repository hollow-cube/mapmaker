package net.hollowcube.map.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

final class BlockHandlerHelpers {
    private static final Tag<NBT> BLOCK_ENTITY_TAG = Tag.NBT("BlockEntityTag");

    /**
     * @param placement
     * @return true if the block data was applied, false otherwise
     */
    public static boolean applyStoredBlockData(@NotNull BlockHandler.PlayerPlacement placement) {
        var itemStack = placement.getPlayer().getItemInHand(placement.getHand());
        var blockData = extractBlockData(itemStack);
        if (blockData == null) return false;

        // Block data was present, apply it
        placement.getInstance().setBlock(
                placement.getBlockPosition(),
                placement.getBlock().withNbt(blockData)
        );
        return true;
    }

    public static @Nullable NBTCompound extractBlockData(@NotNull ItemStack itemStack) {
        var nbt = itemStack.getTag(BLOCK_ENTITY_TAG);
        if (!(nbt instanceof NBTCompound compound)) return null;
        return compound.withRemovedKeys("id", "x", "y", "z");
    }
}
