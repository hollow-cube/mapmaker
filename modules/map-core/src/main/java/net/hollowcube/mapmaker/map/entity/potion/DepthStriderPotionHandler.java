package net.hollowcube.mapmaker.map.entity.potion;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.enchant.LevelBasedValue;

public class DepthStriderPotionHandler implements PotionHandler {
    private static final Key MODIFIER_ID = Key.key("mapmaker:effect.depth_strider");
    private static final LevelBasedValue VALUE = new LevelBasedValue.Linear(0.33333334f, 0.33333334f);

    @Override
    public void apply(Player player, int level) {
        var modifier = new AttributeModifier(MODIFIER_ID, VALUE.calc(level + 1), AttributeOperation.ADD_VALUE);
        final AttributeInstance attr = player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY);
        attr.removeModifier(MODIFIER_ID);
        attr.addModifier(modifier);
    }

    @Override
    public void remove(Player player) {
        player.getAttribute(Attribute.WATER_MOVEMENT_EFFICIENCY).removeModifier(MODIFIER_ID);
    }
}
