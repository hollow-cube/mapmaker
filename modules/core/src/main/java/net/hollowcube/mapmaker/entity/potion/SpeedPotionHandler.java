package net.hollowcube.mapmaker.entity.potion;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

class SpeedPotionHandler implements PotionHandler {
    private static final UUID MODIFIER_ID = UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635");

    @Override
    public void apply(@NotNull Player player, int level) {
        var modifier = new AttributeModifier(MODIFIER_ID, "speed_potion",
                0.2 * (level + 1), AttributeOperation.MULTIPLY_TOTAL);
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).addModifier(modifier);
    }

    @Override
    public void remove(@NotNull Player player) {
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(MODIFIER_ID);
    }
}
