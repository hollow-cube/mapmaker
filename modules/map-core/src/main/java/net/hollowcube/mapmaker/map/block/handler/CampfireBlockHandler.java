package net.hollowcube.mapmaker.map.block.handler;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class CampfireBlockHandler implements BlockHandler {
    public static final Key ID = Key.key("minecraft:campfire");

    private static final List<ItemStack> DEFAULT_ITEMS = List.of(ItemStack.AIR, ItemStack.AIR, ItemStack.AIR, ItemStack.AIR);
    private static final Tag<List<ItemStack>> ITEMS_TAG = Tag.View(new ItemListSerializer()).defaultValue(DEFAULT_ITEMS);

    @Override
    public @NotNull Key getKey() {
        return ID;
    }

    private @NotNull Block setItemStack(@NotNull Block block, int index, @NotNull ItemStack newItemStack) {
        var items = new ArrayList<>(block.getTag(ITEMS_TAG));
        items.set(index, newItemStack);
        return block.withTag(ITEMS_TAG, items);
    }

    @Override
    public boolean onInteract(@NotNull BlockHandler.Interaction interaction) {
        var player = interaction.getPlayer();
        if (interaction.getHand() != PlayerHand.MAIN) return true;
        if (player.isSneaking()) return true;

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
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(ITEMS_TAG);
    }

    private static int getClickIndex(@NotNull Point cursorPosition, @NotNull Block block) {
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

    @SuppressWarnings("UnstableApiUsage")
    private static final class ItemListSerializer implements TagSerializer<List<ItemStack>> {
        private final Tag<List<BinaryTag>> RAW = Tag.NBT("Items").list();

        @Override
        public @Nullable List<ItemStack> read(@NotNull TagReadable tag) {
            var raw = tag.getTag(RAW);
            if (raw == null) return null;

            var items = new ArrayList<ItemStack>();
            for (var nbt : raw) {
                items.add(ItemStack.fromItemNBT((CompoundBinaryTag) nbt));
            }
            return items;
        }

        @Override
        public void write(@NotNull TagWritable tagWritable, @NotNull List<ItemStack> itemStacks) {
            var raw = new ArrayList<BinaryTag>();
            for (int i = 0; i < Math.min(itemStacks.size(), 4); i++) {
                raw.add(itemStacks.get(i).toItemNBT().putByte("Slot", (byte) i));
            }
            tagWritable.setTag(RAW, raw);
        }
    }
}
