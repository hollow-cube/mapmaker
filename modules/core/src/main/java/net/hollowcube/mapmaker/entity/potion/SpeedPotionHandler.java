package net.hollowcube.mapmaker.entity.potion;

import net.minestom.server.attribute.Attribute;
import net.minestom.server.attribute.AttributeModifier;
import net.minestom.server.attribute.AttributeOperation;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

class SpeedPotionHandler implements PotionHandler {
    private static final UUID MODIFIER_ID = UUID.fromString("a3634428-40a0-45b3-8583-a3b5813d64c5");

    @Override
    public void apply(@NotNull Player player, int level) {
        System.out.println("ADD SPEED TO " + player.getUsername() + " LEVEL " + level);
        var modifier = new AttributeModifier(MODIFIER_ID, "speed_potion",
                0.2 * (level + 1), AttributeOperation.MULTIPLY_TOTAL);
        player.getAttribute(Attribute.MOVEMENT_SPEED).addModifier(modifier);
    }

    @Override
    public void remove(@NotNull Player player) {
        System.out.println("REMOVE SPEED FROM " + player.getUsername());
        player.getAttribute(Attribute.MOVEMENT_SPEED).removeModifier(MODIFIER_ID);
    }
}
