package net.hollowcube.mapmaker.entity.potion;

import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public record GenericModifierPotionHandler(
        @NotNull Attribute attribute,
        @NotNull NamespaceID modifierId,
        @NotNull Int2DoubleFunction formula,
        @NotNull AttributeOperation operation
) implements PotionHandler {

    @Override
    public void apply(@NotNull Player player, int level) {
        var modifier = new AttributeModifier(modifierId, formula.apply(level), operation);
        final AttributeInstance attr = player.getAttribute(attribute);
        attr.removeModifier(modifier);
        attr.addModifier(modifier);
    }

    @Override
    public void remove(@NotNull Player player) {
        player.getAttribute(attribute).removeModifier(modifierId);
    }
}
