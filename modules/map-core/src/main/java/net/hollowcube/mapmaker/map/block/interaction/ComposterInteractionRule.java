package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.item.ItemTags;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class ComposterInteractionRule implements BlockInteractionRule {
    private static final Collection<NamespaceID> COMPOSTABLE_BLOCKS = new HashSet<>();

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        // We can go to the next state if either the block is in the list of compostable blocks, or it is at the max level.
        var level = Integer.parseInt(block.getProperty("level"));
        if (level != 8 && !COMPOSTABLE_BLOCKS.contains(interaction.item().material().namespace()))
            return false;

        var newLevel = String.valueOf((level + 1) % 9);
        interaction.setBlock(blockPosition, block.withProperty("level", newLevel));
        return true;
    }

    static {
        COMPOSTABLE_BLOCKS.addAll(ItemTags.LEAVES);
        COMPOSTABLE_BLOCKS.addAll(ItemTags.SAPLINGS);
        COMPOSTABLE_BLOCKS.add(Material.BEETROOT_SEEDS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.DRIED_KELP.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SHORT_GRASS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.KELP.namespace());
        COMPOSTABLE_BLOCKS.add(Material.MELON_SEEDS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PUMPKIN_SEEDS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SEAGRASS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SWEET_BERRIES.namespace());
        COMPOSTABLE_BLOCKS.add(Material.GLOW_BERRIES.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WHEAT_SEEDS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.MOSS_CARPET.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PINK_PETALS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SMALL_DRIPLEAF.namespace());
        COMPOSTABLE_BLOCKS.add(Material.HANGING_ROOTS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.MANGROVE_ROOTS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.TORCHFLOWER_SEEDS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PITCHER_POD.namespace());
        COMPOSTABLE_BLOCKS.add(Material.DRIED_KELP_BLOCK.namespace());
        COMPOSTABLE_BLOCKS.add(Material.TALL_GRASS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.CACTUS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SUGAR_CANE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.VINE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.NETHER_SPROUTS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WEEPING_VINES.namespace());
        COMPOSTABLE_BLOCKS.add(Material.TWISTING_VINES.namespace());
        COMPOSTABLE_BLOCKS.add(Material.MELON_SLICE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.GLOW_LICHEN.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SEA_PICKLE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.LILY_PAD.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PUMPKIN.namespace());
        COMPOSTABLE_BLOCKS.add(Material.CARVED_PUMPKIN.namespace());
        COMPOSTABLE_BLOCKS.add(Material.MELON.namespace());
        COMPOSTABLE_BLOCKS.add(Material.APPLE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.BEETROOT.namespace());
        COMPOSTABLE_BLOCKS.add(Material.CARROT.namespace());
        COMPOSTABLE_BLOCKS.add(Material.COCOA_BEANS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.POTATO.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WHEAT.namespace());
        COMPOSTABLE_BLOCKS.add(Material.BROWN_MUSHROOM.namespace());
        COMPOSTABLE_BLOCKS.add(Material.RED_MUSHROOM.namespace());
        COMPOSTABLE_BLOCKS.add(Material.MUSHROOM_STEM.namespace());
        COMPOSTABLE_BLOCKS.add(Material.CRIMSON_FUNGUS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WARPED_FUNGUS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.NETHER_WART.namespace());
        COMPOSTABLE_BLOCKS.add(Material.CRIMSON_ROOTS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WARPED_ROOTS.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SHROOMLIGHT.namespace());
        COMPOSTABLE_BLOCKS.add(Material.DANDELION.namespace());
        COMPOSTABLE_BLOCKS.add(Material.POPPY.namespace());
        COMPOSTABLE_BLOCKS.add(Material.BLUE_ORCHID.namespace());
        COMPOSTABLE_BLOCKS.add(Material.ALLIUM.namespace());
        COMPOSTABLE_BLOCKS.add(Material.AZURE_BLUET.namespace());
        COMPOSTABLE_BLOCKS.add(Material.RED_TULIP.namespace());
        COMPOSTABLE_BLOCKS.add(Material.ORANGE_TULIP.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WHITE_TULIP.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PINK_TULIP.namespace());
        COMPOSTABLE_BLOCKS.add(Material.OXEYE_DAISY.namespace());
        COMPOSTABLE_BLOCKS.add(Material.CORNFLOWER.namespace());
        COMPOSTABLE_BLOCKS.add(Material.LILY_OF_THE_VALLEY.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WITHER_ROSE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.FERN.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SUNFLOWER.namespace());
        COMPOSTABLE_BLOCKS.add(Material.LILAC.namespace());
        COMPOSTABLE_BLOCKS.add(Material.ROSE_BUSH.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PEONY.namespace());
        COMPOSTABLE_BLOCKS.add(Material.LARGE_FERN.namespace());
        COMPOSTABLE_BLOCKS.add(Material.SPORE_BLOSSOM.namespace());
        COMPOSTABLE_BLOCKS.add(Material.AZALEA.namespace());
        COMPOSTABLE_BLOCKS.add(Material.MOSS_BLOCK.namespace());
        COMPOSTABLE_BLOCKS.add(Material.BIG_DRIPLEAF.namespace());
        COMPOSTABLE_BLOCKS.add(Material.HAY_BLOCK.namespace());
        COMPOSTABLE_BLOCKS.add(Material.BROWN_MUSHROOM_BLOCK.namespace());
        COMPOSTABLE_BLOCKS.add(Material.RED_MUSHROOM_BLOCK.namespace());
        COMPOSTABLE_BLOCKS.add(Material.NETHER_WART_BLOCK.namespace());
        COMPOSTABLE_BLOCKS.add(Material.WARPED_WART_BLOCK.namespace());
        COMPOSTABLE_BLOCKS.add(Material.FLOWERING_AZALEA.namespace());
        COMPOSTABLE_BLOCKS.add(Material.BREAD.namespace());
        COMPOSTABLE_BLOCKS.add(Material.BAKED_POTATO.namespace());
        COMPOSTABLE_BLOCKS.add(Material.COOKIE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.TORCHFLOWER.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PITCHER_PLANT.namespace());
        COMPOSTABLE_BLOCKS.add(Material.CAKE.namespace());
        COMPOSTABLE_BLOCKS.add(Material.PUMPKIN_PIE.namespace());
    }
}
