package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.item.ItemTags;
import net.kyori.adventure.key.Key;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class ComposterInteractionRule implements BlockInteractionRule {
    private static final Collection<Key> COMPOSTABLE_BLOCKS = new HashSet<>();

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        // We can go to the next state if either the block is in the list of compostable blocks, or it is at the max level.
        var level = Integer.parseInt(block.getProperty("level"));
        if (level != 8 && !COMPOSTABLE_BLOCKS.contains(interaction.item().material().key()))
            return false;

        var newLevel = String.valueOf((level + 1) % 9);
        interaction.setBlock(blockPosition, block.withProperty("level", newLevel));
        return true;
    }

    static {
        COMPOSTABLE_BLOCKS.addAll(ItemTags.LEAVES);
        COMPOSTABLE_BLOCKS.addAll(ItemTags.SAPLINGS);
        COMPOSTABLE_BLOCKS.add(Material.BEETROOT_SEEDS.key());
        COMPOSTABLE_BLOCKS.add(Material.DRIED_KELP.key());
        COMPOSTABLE_BLOCKS.add(Material.SHORT_GRASS.key());
        COMPOSTABLE_BLOCKS.add(Material.KELP.key());
        COMPOSTABLE_BLOCKS.add(Material.MELON_SEEDS.key());
        COMPOSTABLE_BLOCKS.add(Material.PUMPKIN_SEEDS.key());
        COMPOSTABLE_BLOCKS.add(Material.SEAGRASS.key());
        COMPOSTABLE_BLOCKS.add(Material.SWEET_BERRIES.key());
        COMPOSTABLE_BLOCKS.add(Material.GLOW_BERRIES.key());
        COMPOSTABLE_BLOCKS.add(Material.WHEAT_SEEDS.key());
        COMPOSTABLE_BLOCKS.add(Material.MOSS_CARPET.key());
        COMPOSTABLE_BLOCKS.add(Material.PINK_PETALS.key());
        COMPOSTABLE_BLOCKS.add(Material.SMALL_DRIPLEAF.key());
        COMPOSTABLE_BLOCKS.add(Material.HANGING_ROOTS.key());
        COMPOSTABLE_BLOCKS.add(Material.MANGROVE_ROOTS.key());
        COMPOSTABLE_BLOCKS.add(Material.TORCHFLOWER_SEEDS.key());
        COMPOSTABLE_BLOCKS.add(Material.PITCHER_POD.key());
        COMPOSTABLE_BLOCKS.add(Material.DRIED_KELP_BLOCK.key());
        COMPOSTABLE_BLOCKS.add(Material.TALL_GRASS.key());
        COMPOSTABLE_BLOCKS.add(Material.CACTUS.key());
        COMPOSTABLE_BLOCKS.add(Material.SUGAR_CANE.key());
        COMPOSTABLE_BLOCKS.add(Material.VINE.key());
        COMPOSTABLE_BLOCKS.add(Material.NETHER_SPROUTS.key());
        COMPOSTABLE_BLOCKS.add(Material.WEEPING_VINES.key());
        COMPOSTABLE_BLOCKS.add(Material.TWISTING_VINES.key());
        COMPOSTABLE_BLOCKS.add(Material.MELON_SLICE.key());
        COMPOSTABLE_BLOCKS.add(Material.GLOW_LICHEN.key());
        COMPOSTABLE_BLOCKS.add(Material.SEA_PICKLE.key());
        COMPOSTABLE_BLOCKS.add(Material.LILY_PAD.key());
        COMPOSTABLE_BLOCKS.add(Material.PUMPKIN.key());
        COMPOSTABLE_BLOCKS.add(Material.CARVED_PUMPKIN.key());
        COMPOSTABLE_BLOCKS.add(Material.MELON.key());
        COMPOSTABLE_BLOCKS.add(Material.APPLE.key());
        COMPOSTABLE_BLOCKS.add(Material.BEETROOT.key());
        COMPOSTABLE_BLOCKS.add(Material.CARROT.key());
        COMPOSTABLE_BLOCKS.add(Material.COCOA_BEANS.key());
        COMPOSTABLE_BLOCKS.add(Material.POTATO.key());
        COMPOSTABLE_BLOCKS.add(Material.WHEAT.key());
        COMPOSTABLE_BLOCKS.add(Material.BROWN_MUSHROOM.key());
        COMPOSTABLE_BLOCKS.add(Material.RED_MUSHROOM.key());
        COMPOSTABLE_BLOCKS.add(Material.MUSHROOM_STEM.key());
        COMPOSTABLE_BLOCKS.add(Material.CRIMSON_FUNGUS.key());
        COMPOSTABLE_BLOCKS.add(Material.WARPED_FUNGUS.key());
        COMPOSTABLE_BLOCKS.add(Material.NETHER_WART.key());
        COMPOSTABLE_BLOCKS.add(Material.CRIMSON_ROOTS.key());
        COMPOSTABLE_BLOCKS.add(Material.WARPED_ROOTS.key());
        COMPOSTABLE_BLOCKS.add(Material.SHROOMLIGHT.key());
        COMPOSTABLE_BLOCKS.add(Material.DANDELION.key());
        COMPOSTABLE_BLOCKS.add(Material.POPPY.key());
        COMPOSTABLE_BLOCKS.add(Material.BLUE_ORCHID.key());
        COMPOSTABLE_BLOCKS.add(Material.ALLIUM.key());
        COMPOSTABLE_BLOCKS.add(Material.AZURE_BLUET.key());
        COMPOSTABLE_BLOCKS.add(Material.RED_TULIP.key());
        COMPOSTABLE_BLOCKS.add(Material.ORANGE_TULIP.key());
        COMPOSTABLE_BLOCKS.add(Material.WHITE_TULIP.key());
        COMPOSTABLE_BLOCKS.add(Material.PINK_TULIP.key());
        COMPOSTABLE_BLOCKS.add(Material.OXEYE_DAISY.key());
        COMPOSTABLE_BLOCKS.add(Material.CORNFLOWER.key());
        COMPOSTABLE_BLOCKS.add(Material.LILY_OF_THE_VALLEY.key());
        COMPOSTABLE_BLOCKS.add(Material.WITHER_ROSE.key());
        COMPOSTABLE_BLOCKS.add(Material.FERN.key());
        COMPOSTABLE_BLOCKS.add(Material.SUNFLOWER.key());
        COMPOSTABLE_BLOCKS.add(Material.LILAC.key());
        COMPOSTABLE_BLOCKS.add(Material.ROSE_BUSH.key());
        COMPOSTABLE_BLOCKS.add(Material.PEONY.key());
        COMPOSTABLE_BLOCKS.add(Material.LARGE_FERN.key());
        COMPOSTABLE_BLOCKS.add(Material.SPORE_BLOSSOM.key());
        COMPOSTABLE_BLOCKS.add(Material.AZALEA.key());
        COMPOSTABLE_BLOCKS.add(Material.MOSS_BLOCK.key());
        COMPOSTABLE_BLOCKS.add(Material.BIG_DRIPLEAF.key());
        COMPOSTABLE_BLOCKS.add(Material.HAY_BLOCK.key());
        COMPOSTABLE_BLOCKS.add(Material.BROWN_MUSHROOM_BLOCK.key());
        COMPOSTABLE_BLOCKS.add(Material.RED_MUSHROOM_BLOCK.key());
        COMPOSTABLE_BLOCKS.add(Material.NETHER_WART_BLOCK.key());
        COMPOSTABLE_BLOCKS.add(Material.WARPED_WART_BLOCK.key());
        COMPOSTABLE_BLOCKS.add(Material.FLOWERING_AZALEA.key());
        COMPOSTABLE_BLOCKS.add(Material.BREAD.key());
        COMPOSTABLE_BLOCKS.add(Material.BAKED_POTATO.key());
        COMPOSTABLE_BLOCKS.add(Material.COOKIE.key());
        COMPOSTABLE_BLOCKS.add(Material.TORCHFLOWER.key());
        COMPOSTABLE_BLOCKS.add(Material.PITCHER_PLANT.key());
        COMPOSTABLE_BLOCKS.add(Material.CAKE.key());
        COMPOSTABLE_BLOCKS.add(Material.PUMPKIN_PIE.key());
    }
}
