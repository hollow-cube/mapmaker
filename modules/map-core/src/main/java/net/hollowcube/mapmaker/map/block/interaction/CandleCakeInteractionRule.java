package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;

public class CandleCakeInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var block = interaction.getBlock(interaction.blockPosition());
        var lit = block.getProperty("lit").equals("true");
        var item = interaction.item().material();
        var cursor = interaction.cursorPosition();

        if (lit && item == Material.AIR && (cursor == null || cursor.y() > 0.5)) {
            interaction.setBlock(interaction.blockPosition(), block.withProperty("lit", "false"));
            return true;
        } else if (!lit && (item == Material.FLINT_AND_STEEL || item == Material.FIRE_CHARGE)) {
            interaction.setBlock(interaction.blockPosition(), block.withProperty("lit", "true"));
            return true;
        }

        // If we interact with a candle cake, it converts the candle cake back into a normal cake
        // with one slice eaten
        var replacement = Block.CAKE.withProperty("bites", "1");
        interaction.setBlock(interaction.blockPosition(), replacement);

        return true;
    }
}
