package net.hollowcube.mapmaker.map.block.handler;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockHandlerHelpers {

    /**
     * @param placement
     * @return true if the block data was applied, false otherwise
     */
    public static boolean applyStoredBlockData(@NotNull BlockHandler.PlayerPlacement placement) {
        var itemStack = placement.getPlayer().getItemInHand(placement.getHand());
        var blockData = extractBlockData(itemStack);
        if (blockData == null) return false;

        // Block data was present, apply it
        updateBlock(placement, blockData);
        return true;
    }

    public static boolean applyStoredBlockData(PlayerBlockPlaceEvent event) {
        var itemStack = event.getPlayer().getItemInHand(event.getHand());
        var blockData = extractBlockData(itemStack);
        if (blockData == null) return false;

        event.setBlock(event.getBlock().withNbt(blockData));
        return true;
    }

    /**
     * @param placement
     * @return true if the block data was applied, false otherwise
     */
    public static void applyItemData(@NotNull BlockHandler.PlayerPlacement placement) {
        var itemStack = placement.getPlayer().getItemInHand(placement.getHand());
        var blockData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).nbt();
        if (blockData.size() == 0) return;
        updateBlock(placement, blockData);
    }

    public static @Nullable CompoundBinaryTag extractBlockData(@NotNull ItemStack itemStack) {
        var blockData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).nbt();
        if (blockData.size() == 0) return null;
        var builder = CompoundBinaryTag.builder();
        builder.put(blockData);
        builder.remove("id");
        builder.remove("x");
        builder.remove("y");
        builder.remove("z");
        return builder.build();
    }

    public static void updateBlock(@NotNull BlockHandler.PlayerPlacement placement, @NotNull CompoundBinaryTag blockData) {
        // Note that we refetch the block from instance rather than use the one in `placement`. This is because the
        // one in `placement` was not updated after the placement rule changed it.
        // todo this is realistically a Minestom bug that should be fixed
        var instance = placement.getInstance();
        var block = instance.getBlock(placement.getBlockPosition());
        instance.setBlock(placement.getBlockPosition(), block.withNbt(blockData));
    }
}
