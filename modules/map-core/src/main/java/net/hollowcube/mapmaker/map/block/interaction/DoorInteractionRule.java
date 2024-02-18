package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.block.BlockTags;
import org.jetbrains.annotations.NotNull;

public class DoorInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        // Set the open value of the door
        var newValue = String.valueOf("false".equals(block.getProperty("open")));
        interaction.setBlock(blockPosition, block.withProperty("open", newValue));

        // If there is another half of the door, open that too.
        var isTopHalf = block.getProperty("half").equalsIgnoreCase("upper");
        var otherPosition = blockPosition.add(0, isTopHalf ? -1 : 1, 0);
        var otherBlock = interaction.getBlock(otherPosition);
        if (BlockTags.DOORS.contains(otherBlock.namespace())) {
            interaction.setBlock(otherPosition, otherBlock.withProperty("open", newValue));
        }

        return true;
    }
}
