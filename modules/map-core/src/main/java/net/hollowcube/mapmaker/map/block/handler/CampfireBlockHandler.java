package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.mapmaker.map.block.handler.base.ContainerSerializer;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class CampfireBlockHandler implements BlockHandler {
    public static final Key ID = Key.key("minecraft:campfire");

    private static final List<ItemStack> DEFAULT_ITEMS = List.of(ItemStack.AIR, ItemStack.AIR, ItemStack.AIR, ItemStack.AIR);
    private static final Tag<List<ItemStack>> ITEMS_TAG = Tag.View(new ContainerSerializer(4)).defaultValue(DEFAULT_ITEMS);

    @Override
    public Key getKey() {
        return ID;
    }

    private Block setItemStack(Block block, int index, ItemStack newItemStack) {
        var items = new ArrayList<>(block.getTag(ITEMS_TAG));
        items.set(index, newItemStack);
        return block.withTag(ITEMS_TAG, items);
    }

    @Override
    public boolean onInteract(BlockHandler.Interaction interaction) {
        if (!BlockHandlerHelpers.canEdit(interaction)) return true;

        var player = interaction.getPlayer();
        if (interaction.getHand() != PlayerHand.MAIN || player.isSneaking()) return true;

        var handItemStack = player.getItemInHand(interaction.getHand());

        var block = interaction.getBlock();
        int index = getClickIndex(interaction.getCursorPosition(), block);
        var items = block.getTag(ITEMS_TAG);
        if (index < items.size() && !items.get(index).isAir()) {
            // Item is present, remove it
            block = setItemStack(block, index, ItemStack.AIR);
        } else if (!handItemStack.isAir()) {
            // Player has an item, insert it
            block = setItemStack(block, index, handItemStack);
        } else return true; // Unhandled

        // Write back the modified block (with new data)
        interaction.getInstance().setBlock(interaction.getBlockPosition(), block);
        return false;
    }

    @Override
    public Collection<Tag<?>> getBlockEntityTags() {
        return List.of(ITEMS_TAG);
    }

    private static int getClickIndex(Point cursorPosition, Block block) {
        int raw;
        if (cursorPosition.x() > 0.5)
            raw = cursorPosition.z() > 0.5 ? 0 : 3;
        else raw = cursorPosition.z() > 0.5 ? 1 : 2;

        var facing = BlockFace.valueOf(block.getProperty("facing").toUpperCase(Locale.ROOT));
        return (raw + switch (facing) {
            case NORTH -> 0;
            case EAST -> 3;
            case SOUTH -> 2;
            case WEST -> 1;
            default -> throw new IllegalArgumentException("unreachable");
        }) % 4;
    }
}
