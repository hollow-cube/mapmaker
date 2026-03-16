package net.hollowcube.mapmaker.map.block.interaction;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

public class CakeInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        var bites = Integer.parseInt(block.getProperty("bites"));
        var item = interaction.item();

        if (bites == 0 && item.material().name().contains("candle")) {
            // If we interact with a candle and the cake is not eaten
            Block candleCake = convertCandleToCake(item.material());
            if (candleCake == null) {
                // We have a candle that can't be mapped to a cake, so we can't place it
                return false;
            }

            interaction.setBlock(blockPosition, candleCake);
            return true;
        }

        block = block.withProperty("bites", String.valueOf((bites + 1) % 7));
        interaction.setBlock(blockPosition, block);

        return true;
    }

    private static @Nullable Block convertCandleToCake(Material material) {
        // The corresponding cake for a candle is just "x_candle_cake", e.g. "white_candle" -> "white_candle_cake"
        // This is also true for the normal candle, which is just "candle" -> "candle_cake"
        String blockName = material.key().value() + "_cake";
        return Block.fromKey(Key.key(Key.MINECRAFT_NAMESPACE, blockName));
    }
}
