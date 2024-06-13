package net.hollowcube.mapmaker.entity.potion;

import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.enchant.LevelBasedValue;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public class DepthStriderPotionHandler implements PotionHandler {
    private static final NamespaceID MODIFIER_ID = NamespaceID.from("mapmaker:effect.depth_strider");
    private static final LevelBasedValue VALUE = new LevelBasedValue.Linear(0.33333334f, 0.33333334f);

    @Override
    public void apply(@NotNull Player player, int level) {
        var modifier = new AttributeModifier(MODIFIER_ID, VALUE.calc(level + 1), AttributeOperation.ADD_VALUE);
        player.getAttribute(Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY).addModifier(modifier);
    }

    @Override
    public void remove(@NotNull Player player) {
        player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).removeModifier(MODIFIER_ID);
    }
}
