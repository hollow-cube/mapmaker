package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.common.util.PropertyUtil;
import net.hollowcube.mapmaker.map.block.handler.base.ContainerSerializer;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ShelfBlockHandler implements BlockHandler {
    public static final Key ID = Key.key("minecraft:shelf");

    private static final float PIXEL = 0.0625f;
    private static final float SLOT_WIDTH = 4 * PIXEL;

    private static final List<ItemStack> DEFAULT_ITEMS = List.of(ItemStack.AIR, ItemStack.AIR, ItemStack.AIR);
    private static final Tag<List<ItemStack>> ITEMS_TAG = Tag.View(new ContainerSerializer(3)).defaultValue(DEFAULT_ITEMS);
    private static final Tag<Boolean> ALIGN_ITEMS_TO_BOTTOM = Tag.Boolean("align_items_to_bottom").defaultValue(false);

    @Override
    public @NotNull Key getKey() {
        return ID;
    }

    private @NotNull Block setItemStack(@NotNull Block block, int index, @NotNull ItemStack newItemStack) {
        var old = block.getTag(ITEMS_TAG);
        if (index < 0 || index >= old.size()) return block;
        if (!old.get(index).isAir() && !newItemStack.isAir()) return block;

        var items = new ArrayList<>(old);
        items.set(index, newItemStack);
        return block.withTag(ITEMS_TAG, items);
    }

    @Override
    public boolean onInteract(@NotNull BlockHandler.Interaction interaction) {
        var blockPos = interaction.getBlockPosition();
        var block = interaction.getBlock();
        var stack = interaction.getPlayer().getItemInHand(interaction.getHand());
        var direction = PropertyUtil.getFacing(block.properties());
        var cursor = interaction.getCursorPosition();

        if (direction != interaction.getBlockFace().toDirection()) return false;

        if (cursor.y() > 0.25f && cursor.y() < 0.75f) {
            var value = switch (direction) {
                case NORTH -> 1 - cursor.x();
                case SOUTH -> cursor.x();
                case WEST -> cursor.z();
                case EAST -> 1 - cursor.z();
                default -> 0;
            };

            for (int i = 0; i < 3; i++) {
                float start = PIXEL + i * (SLOT_WIDTH + PIXEL);
                float end = start + SLOT_WIDTH;
                if (value >= start && value <= end) {
                    block = setItemStack(block, i, stack);
                    interaction.getInstance().setBlock(blockPos, block);
                    return true;
                }
            }
        }

        if (stack.isAir()) {
            block = block.withTag(ALIGN_ITEMS_TO_BOTTOM, !block.getTag(ALIGN_ITEMS_TO_BOTTOM));
            interaction.getInstance().setBlock(blockPos, block);
            return true;
        }

        return true;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(ITEMS_TAG, ALIGN_ITEMS_TO_BOTTOM);
    }
}
