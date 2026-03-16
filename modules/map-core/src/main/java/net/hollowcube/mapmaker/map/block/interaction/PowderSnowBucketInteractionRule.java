package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.instance.block.Block;

public class PowderSnowBucketInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        if (BlockTags.CAULDRONS.contains(block.key()) && !interaction.player().isSneaking()) {
            interaction.setBlock(blockPosition, Block.POWDER_SNOW_CAULDRON.withProperty("level", "3"));
            return true;
        }
        return false;
    }

}
