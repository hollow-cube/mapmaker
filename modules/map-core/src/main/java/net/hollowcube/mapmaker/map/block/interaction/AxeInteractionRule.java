package net.hollowcube.mapmaker.map.block.interaction;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import net.minestom.server.instance.block.Block;

import java.util.Objects;

public class AxeInteractionRule implements BlockInteractionRule {
    public static final AxeInteractionRule INSTANCE = new AxeInteractionRule();

    private static final Int2IntArrayMap STRIP_MAP; // Map of pre -> post right click, and reverse

    private AxeInteractionRule() {
    }

    @Override
    public SneakState sneakState() {
        return SneakState.BOTH;
    }

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        int strippedBlockId = STRIP_MAP.get(block.id());
        if (strippedBlockId == 0) return false;

        var strippedBlock = Objects.requireNonNull(Block.fromBlockId(strippedBlockId))
                .withProperties(block.properties())
                .withNbt(block.nbt()).withHandler(block.handler());
        interaction.setBlock(blockPosition, strippedBlock);
        return false;
    }

    static {
        STRIP_MAP = new Int2IntArrayMap();
        // Log Types
        STRIP_MAP.put(Block.OAK_LOG.id(), Block.STRIPPED_OAK_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_OAK_LOG.id(), Block.OAK_LOG.id());
        STRIP_MAP.put(Block.SPRUCE_LOG.id(), Block.STRIPPED_SPRUCE_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_SPRUCE_LOG.id(), Block.SPRUCE_LOG.id());
        STRIP_MAP.put(Block.BIRCH_LOG.id(), Block.STRIPPED_BIRCH_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_BIRCH_LOG.id(), Block.BIRCH_LOG.id());
        STRIP_MAP.put(Block.JUNGLE_LOG.id(), Block.STRIPPED_JUNGLE_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_JUNGLE_LOG.id(), Block.JUNGLE_LOG.id());
        STRIP_MAP.put(Block.ACACIA_LOG.id(), Block.STRIPPED_ACACIA_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_ACACIA_LOG.id(), Block.ACACIA_LOG.id());
        STRIP_MAP.put(Block.DARK_OAK_LOG.id(), Block.STRIPPED_DARK_OAK_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_DARK_OAK_LOG.id(), Block.DARK_OAK_LOG.id());
        STRIP_MAP.put(Block.MANGROVE_LOG.id(), Block.STRIPPED_MANGROVE_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_MANGROVE_LOG.id(), Block.MANGROVE_LOG.id());
        STRIP_MAP.put(Block.CHERRY_LOG.id(), Block.STRIPPED_CHERRY_LOG.id());
        STRIP_MAP.put(Block.STRIPPED_CHERRY_LOG.id(), Block.CHERRY_LOG.id());
        STRIP_MAP.put(Block.WARPED_STEM.id(), Block.STRIPPED_WARPED_STEM.id());
        STRIP_MAP.put(Block.STRIPPED_WARPED_STEM.id(), Block.WARPED_STEM.id());
        STRIP_MAP.put(Block.CRIMSON_STEM.id(), Block.STRIPPED_CRIMSON_STEM.id());
        STRIP_MAP.put(Block.STRIPPED_CRIMSON_STEM.id(), Block.CRIMSON_STEM.id());

        // Wood Types
        STRIP_MAP.put(Block.OAK_WOOD.id(), Block.STRIPPED_OAK_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_OAK_WOOD.id(), Block.OAK_WOOD.id());
        STRIP_MAP.put(Block.SPRUCE_WOOD.id(), Block.STRIPPED_SPRUCE_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_SPRUCE_WOOD.id(), Block.SPRUCE_WOOD.id());
        STRIP_MAP.put(Block.BIRCH_WOOD.id(), Block.STRIPPED_BIRCH_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_BIRCH_WOOD.id(), Block.BIRCH_WOOD.id());
        STRIP_MAP.put(Block.JUNGLE_WOOD.id(), Block.STRIPPED_JUNGLE_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_JUNGLE_WOOD.id(), Block.JUNGLE_WOOD.id());
        STRIP_MAP.put(Block.ACACIA_WOOD.id(), Block.STRIPPED_ACACIA_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_ACACIA_WOOD.id(), Block.ACACIA_WOOD.id());
        STRIP_MAP.put(Block.DARK_OAK_WOOD.id(), Block.STRIPPED_DARK_OAK_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_DARK_OAK_WOOD.id(), Block.DARK_OAK_WOOD.id());
        STRIP_MAP.put(Block.MANGROVE_WOOD.id(), Block.STRIPPED_MANGROVE_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_MANGROVE_WOOD.id(), Block.MANGROVE_WOOD.id());
        STRIP_MAP.put(Block.CHERRY_WOOD.id(), Block.STRIPPED_CHERRY_WOOD.id());
        STRIP_MAP.put(Block.STRIPPED_CHERRY_WOOD.id(), Block.CHERRY_WOOD.id());
        STRIP_MAP.put(Block.WARPED_HYPHAE.id(), Block.STRIPPED_WARPED_HYPHAE.id());
        STRIP_MAP.put(Block.STRIPPED_WARPED_HYPHAE.id(), Block.WARPED_HYPHAE.id());
        STRIP_MAP.put(Block.CRIMSON_HYPHAE.id(), Block.STRIPPED_CRIMSON_HYPHAE.id());
        STRIP_MAP.put(Block.STRIPPED_CRIMSON_HYPHAE.id(), Block.CRIMSON_HYPHAE.id());
    }

}
