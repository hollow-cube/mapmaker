package net.hollowcube.mapmaker.map.block.handler;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTByte;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.*;

public class CampfireBlockHandler implements BlockHandler {
    public static final NamespaceID ID = NamespaceID.from("minecraft:campfire");

    private static final List<ItemStack> DEFAULT_ITEMS = List.of(ItemStack.AIR, ItemStack.AIR, ItemStack.AIR, ItemStack.AIR);
    private static final Tag<List<ItemStack>> ITEMS_TAG = Tag.View(new ItemListSerializer()).defaultValue(DEFAULT_ITEMS);

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    private @NotNull Block setItemStack(@NotNull Block block, int index, @NotNull ItemStack newItemStack) {
        var items = new ArrayList<>(block.getTag(ITEMS_TAG));
        items.set(index, newItemStack);
        return block.withTag(ITEMS_TAG, items);
    }

    @Override
    public boolean onInteract(@NotNull BlockHandler.Interaction interaction) {
        if (interaction.getHand() != Player.Hand.MAIN) return true;
        var handItemStack = interaction.getPlayer().getItemInHand(interaction.getHand());

        var block = interaction.getBlock();
        int index = getClickIndex(interaction.getCursorPosition(), block);
        var itemStack = block.getTag(ITEMS_TAG).get(index);
        if (!itemStack.isAir()) {
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
        private final Tag<List<NBT>> RAW = Tag.NBT("Items").list();

        @Override
        public @Nullable List<ItemStack> read(@NotNull TagReadable tag) {
            var raw = tag.getTag(RAW);
            if (raw == null) return null;

            var items = new ArrayList<ItemStack>();
            for (var nbt : raw) {
                items.add(ItemStack.fromItemNBT((NBTCompound) nbt));
            }
            return items;
        }

        @Override
        public void write(@NotNull TagWritable tagWritable, @NotNull List<ItemStack> itemStacks) {
            var raw = new ArrayList<NBT>();
            for (int i = 0; i < Math.min(itemStacks.size(), 4); i++) {
                var entry = Map.<String, NBT>entry("Slot", new NBTByte((byte) i));
                raw.add(itemStacks.get(i).toItemNBT().withEntries(entry));
            }
            tagWritable.setTag(RAW, raw);
        }
    }
}
