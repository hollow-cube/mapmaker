package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V4293 extends DataVersion {
    private static final float DEFAULT_CHANCE = 0.085F;

    public V4293() {
        super(4293);

        addFix(DataTypes.ENTITY, V4293::fixEntityDropChancesFormat);
    }

    private static @Nullable Value fixEntityDropChancesFormat(Value entity) {
        var dropChances = Value.emptyMap();
        var oldArmorDropChances = entity.remove("ArmorDropChances");
        var oldHandDropChances = entity.remove("HandDropChances");

        copyDropChance(dropChances, "feet", oldArmorDropChances.get(0));
        copyDropChance(dropChances, "legs", oldArmorDropChances.get(1));
        copyDropChance(dropChances, "chest", oldArmorDropChances.get(2));
        copyDropChance(dropChances, "head", oldArmorDropChances.get(3));
        copyDropChance(dropChances, "mainhand", oldHandDropChances.get(0));
        copyDropChance(dropChances, "offhand", oldHandDropChances.get(1));
        copyDropChance(dropChances, "body", entity.remove("body_armor_drop_chance"));

        if (dropChances.size(0) > 0)
            entity.put("drop_chances", dropChances);
        return null;
    }

    private static void copyDropChance(Value to, String slotName, Value maybeDropChance) {
        if (maybeDropChance.as(Number.class, DEFAULT_CHANCE).floatValue() != DEFAULT_CHANCE)
            to.put(slotName, maybeDropChance);
    }

}
