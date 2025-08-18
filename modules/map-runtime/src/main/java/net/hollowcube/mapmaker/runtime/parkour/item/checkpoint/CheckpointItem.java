package net.hollowcube.mapmaker.runtime.parkour.item.checkpoint;

import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.GiveItemAction;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.item.ItemStack;

/// CheckpointItem defines an item available in a checkpoint
///
/// The actual item implementation is handled by an [net.hollowcube.mapmaker.map.item.handler.ItemHandler] implementation.
public interface CheckpointItem {

    StructCodec<? extends CheckpointItem> codec();

    ItemStack createItemStack();

    default CheckpointItem updateFromItemStack(ItemStack itemStack) {
        return this;
    }

    TranslatableComponent thumbnail();

    GiveItemAction.AbstractItemEditor<?> createEditor(ActionList.Ref ref);

}
