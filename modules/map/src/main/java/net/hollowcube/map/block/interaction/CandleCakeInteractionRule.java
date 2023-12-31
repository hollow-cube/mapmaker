package net.hollowcube.map.block.interaction;

import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class CandleCakeInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        // If we interact with a candle cake, it converts the candle cake back into a normal cake
        // with one slice eaten
        var replacement = Block.CAKE.withProperty("bites", "1");
        interaction.setBlock(interaction.blockPosition(), replacement);

        return true;
    }
}
