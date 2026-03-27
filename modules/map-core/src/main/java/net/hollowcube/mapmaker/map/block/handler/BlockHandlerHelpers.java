package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.component.DataComponents;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BlockHandlerHelpers {

    public static boolean canEdit(@NotNull BlockHandler.Interaction interaction) {
        var player = interaction.getPlayer();
        var world = MapWorld.forPlayer(player);
        return world != null && world.canEdit(player);
    }

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
        var data = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.nbt().isEmpty()) return;
        updateBlock(placement, data.nbt());
    }

    public static @Nullable CompoundBinaryTag extractBlockData(@NotNull ItemStack itemStack) {
        var data = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.nbt().isEmpty()) return null;
        var builder = CompoundBinaryTag.builder();
        builder.put(data.nbt());
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
