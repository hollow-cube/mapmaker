package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.entity.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V703 extends DataVersion {
    public V703() {
        super(703);

        removeReference(DataType.ENTITY, "EntityHorse");
        addReference(DataType.ENTITY, "Horse", field -> field
                .single("ArmorItem", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "Donkey", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "Mule", field -> field
                .list("Items", DataType.ITEM_STACK)
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "ZombieHorse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "SkeletonHorse", field -> field
                .single("SaddleItem", DataType.ITEM_STACK));

        addFix(DataType.ENTITY, "EntityHorse", new EntityRenameFix(V703::fixEntityHorseSplit));
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
