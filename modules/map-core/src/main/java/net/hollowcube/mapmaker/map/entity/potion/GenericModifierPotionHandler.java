package net.hollowcube.mapmaker.map.entity.potion;

import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;

public record GenericModifierPotionHandler(
        Attribute attribute,
        Key modifierId,
        Int2DoubleFunction formula,
        AttributeOperation operation
) implements PotionHandler {

    @Override
    public void apply(Player player, int level) {
        var modifier = new AttributeModifier(modifierId, formula.apply(level), operation);
        player.getAttribute(attribute).addModifier(modifier);
    }

    @Override
    public void remove(Player player) {
        player.getAttribute(attribute).removeModifier(modifierId);
    }
}
