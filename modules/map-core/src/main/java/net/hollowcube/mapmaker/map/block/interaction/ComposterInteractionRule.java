package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.loot.number.ResolvableInt;
import org.jetbrains.annotations.NotNull;

public class ComposterInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        // We can go to the next state if either the item is compostable, or it is at the max level.
        var level = Integer.parseInt(block.getProperty("level"));
        if (level != 8 && !isCompostable(interaction.item()))
            return false;

        var newLevel = String.valueOf((level + 1) % 9);
        interaction.setBlock(blockPosition, block.withProperty("level", newLevel));
        return true;
    }

    // Vanilla items use provider references which roll a random layer count, but building needs to be
    // deterministic so any compostable item adds exactly one layer. Only a literal 0 can never add one.
    static boolean isCompostable(@NotNull ItemStack item) {
        var compostable = item.get(DataComponents.COMPOSTABLE);
        if (compostable == null) return false;
        return !(compostable.layers() instanceof ResolvableInt.Constant(int layers)) || layers > 0;
    }
}
