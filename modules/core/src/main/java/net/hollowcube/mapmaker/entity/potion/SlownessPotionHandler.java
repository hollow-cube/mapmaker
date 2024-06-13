package net.hollowcube.mapmaker.entity.potion;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

class SlownessPotionHandler implements PotionHandler {
    private static final NamespaceID MODIFIER_ID = NamespaceID.from("minecraft:effect.slowness");

    @Override
    public void apply(@NotNull Player player, int level) {
        var modifier = new AttributeModifier(MODIFIER_ID, -0.15f * (level + 1), AttributeOperation.MULTIPLY_TOTAL);
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
    }

    @Override
    public void remove(@NotNull Player player) {
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(MODIFIER_ID);
    }
}
