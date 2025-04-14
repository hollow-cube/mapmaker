package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V703 extends DataVersion {
    public V703() {
        super(703);

        removeReference(DataTypes.ENTITY, "EntityHorse");
        addReference(DataTypes.ENTITY, "Horse", field -> field
                .single("ArmorItem", DataTypes.ITEM_STACK)
                .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "Donkey", field -> field
                .list("Items", DataTypes.ITEM_STACK)
                .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "Mule", field -> field
                .list("Items", DataTypes.ITEM_STACK)
                .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "ZombieHorse", field -> field
                .single("SaddleItem", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "SkeletonHorse", field -> field
                .single("SaddleItem", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY, "EntityHorse", new EntityRenameFix(V703::fixEntityHorseSplit));
    }

    private static String fixEntityHorseSplit(Value value, String s) {
        int type = value.get("Type").as(Number.class, 0).intValue();
        value.put("Type", null);
        return switch (type) {
            case 1 -> "Donkey";
            case 2 -> "Mule";
            case 3 -> "ZombieHorse";
            case 4 -> "SkeletonHorse";
            default -> "Horse";
        };
    }
}
