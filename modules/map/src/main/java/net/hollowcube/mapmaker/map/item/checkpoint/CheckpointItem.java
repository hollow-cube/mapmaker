package net.hollowcube.mapmaker.map.item.checkpoint;

import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.impl.GiveItemAction;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/// CheckpointItem defines an item available in a checkpoint
///
/// The actual item implementation is handled by an [net.hollowcube.mapmaker.map.item.handler.ItemHandler] implementation.
public interface CheckpointItem {

    @NotNull StructCodec<? extends CheckpointItem> codec();

    default @NotNull ItemStack createItemStack() {
        throw new UnsupportedOperationException("todo");
    }

    default @NotNull GiveItemAction.AbstractItemEditor<?> createEditor(@NotNull ActionList.Ref ref) {
        throw new UnsupportedOperationException("todo");
    }

}
