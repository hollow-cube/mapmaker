package net.hollowcube.map2.block.interaction;

import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CakeInteractionRule implements net.hollowcube.map2.block.interaction.BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
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

    private static @Nullable Block convertCandleToCake(@NotNull Material material) {
        // The corresponding cake for a candle is just "x_candle_cake", e.g. "white_candle" -> "white_candle_cake"
        // This is also true for the normal candle, which is just "candle" -> "candle_cake"
        String blockName = material.namespace().value() + "_cake";
        return Block.fromNamespaceId(NamespaceID.from(NamespaceID.MINECRAFT_NAMESPACE, blockName));
    }
}
