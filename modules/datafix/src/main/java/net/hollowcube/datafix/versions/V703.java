package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

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
    }
}
