package net.hollowcube.mapmaker.util;

import net.kyori.adventure.text.Component;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class ItemUtils {

    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(
            Block.STRUCTURE_BLOCK, Block.JIGSAW, Block.BARRIER,
            Block.COMMAND_BLOCK, Block.CHAIN_COMMAND_BLOCK, Block.REPEATING_COMMAND_BLOCK,
            Block.TEST_BLOCK, Block.TEST_INSTANCE_BLOCK,

            // Blocks with bad prediction
            Block.BEACON, Block.SMITHING_TABLE, Block.CRAFTER, Block.CRAFTING_TABLE, Block.FURNACE, Block.BLAST_FURNACE,
            Block.SMOKER, Block.CARTOGRAPHY_TABLE, Block.LOOM, Block.NOTE_BLOCK, Block.BARREL,
            Block.SHULKER_BOX, Block.BLACK_SHULKER_BOX, Block.BLUE_SHULKER_BOX, Block.BROWN_SHULKER_BOX, Block.CYAN_SHULKER_BOX,
            Block.GRAY_SHULKER_BOX, Block.GREEN_SHULKER_BOX, Block.LIGHT_BLUE_SHULKER_BOX, Block.LIGHT_GRAY_SHULKER_BOX,
            Block.LIME_SHULKER_BOX, Block.MAGENTA_SHULKER_BOX, Block.ORANGE_SHULKER_BOX, Block.PINK_SHULKER_BOX,
            Block.PURPLE_SHULKER_BOX, Block.RED_SHULKER_BOX, Block.WHITE_SHULKER_BOX, Block.YELLOW_SHULKER_BOX,
            Block.DISPENSER, Block.DROPPER, Block.RESPAWN_ANCHOR
    );
    public static final Set<Block> PLACEABLE_BLOCKS;
    static {
        Set<Block> blocks = new HashSet<>();

        // Manual blocks that arnt full blocks but are still placeable
        blocks.add(Block.HONEY_BLOCK);
        blocks.add(Block.SLIME_BLOCK);

        blocksLoop:
        for (var block : Block.values()) {
            var material = block.registry().material();
            if (material == null) continue; // Non block item
            if (!material.key().equals(block.key())) continue; // Weird block item (like flint and steel)
            if (EXCLUDED_BLOCKS.contains(block))
                continue; // Blocks with bad prediction

            // Only add blocks that are full cubes
            var shape = block.registry().collisionShape();
            for (var face : BlockFace.values()) {
                if (!shape.isFaceFull(face))
                    continue blocksLoop;
            }

            blocks.add(block);
        }

        PLACEABLE_BLOCKS = Set.copyOf(blocks);
    }

    private ItemUtils() {
    }

    public static @NotNull Component translation(@NotNull Material material) {
        var key = material.key();
        String prefix = material.isBlock() ? "block" : "item";
        var translationKey = String.format("%s.%s.%s", prefix, key.namespace(), key.value());
        return Component.translatable(translationKey);
    }

    public static ItemStack asDisplay(@NotNull Material material) {
        return asDisplay(material, null);
    }

    public static ItemStack asDisplay(@NotNull Material material, @Nullable String overlay) {
        return OverlayItem.with(ItemStack.builder(Material.STICK), material, overlay).build();
    }
}
