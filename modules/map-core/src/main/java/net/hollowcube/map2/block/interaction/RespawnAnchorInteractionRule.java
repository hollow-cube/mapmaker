package net.hollowcube.map2.block.interaction;

import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class RespawnAnchorInteractionRule implements net.hollowcube.map2.block.interaction.BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        if (interaction.item().material().id() != Material.GLOWSTONE.id())
            return false;

        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var charges = Integer.parseInt(block.getProperty("charges"));
        var newCharges = String.valueOf((charges + 1) % 5);
        interaction.setBlock(blockPosition, block.withProperty("charges", newCharges));

        return true;
    }

}
