package net.hollowcube.mapmaker.entity.potion;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

class SlownessPotionHandler implements PotionHandler {
    private static final UUID MODIFIER_ID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");

    @Override
    public void apply(@NotNull Player player, int level) {
        var modifier = new AttributeModifier(MODIFIER_ID, "slowness_potion",
                -0.15f * (level + 1), AttributeOperation.MULTIPLY_TOTAL);
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
    }

    @Override
    public void remove(@NotNull Player player) {
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(MODIFIER_ID);
    }
}
