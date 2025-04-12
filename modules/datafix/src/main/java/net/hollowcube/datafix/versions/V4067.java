package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V4067 extends DataVersion {
    public V4067() {
        super(4067);

        removeReference(DataType.ENTITY, "minecraft:boat");
        removeReference(DataType.ENTITY, "minecraft:chest_boat");

        addReference(DataType.ENTITY, "minecraft:oak_boat");
        addReference(DataType.ENTITY, "minecraft:spruce_boat");
        addReference(DataType.ENTITY, "minecraft:birch_boat");
        addReference(DataType.ENTITY, "minecraft:jungle_boat");
        addReference(DataType.ENTITY, "minecraft:acacia_boat");
        addReference(DataType.ENTITY, "minecraft:cherry_boat");
        addReference(DataType.ENTITY, "minecraft:dark_oak_boat");
        addReference(DataType.ENTITY, "minecraft:mangrove_boat");
        addReference(DataType.ENTITY, "minecraft:bamboo_raft");
        addReference(DataType.ENTITY, "minecraft:oak_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:spruce_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:birch_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:jungle_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:acacia_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:cherry_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:dark_oak_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:mangrove_chest_boat", V4067::chestBoat);
        addReference(DataType.ENTITY, "minecraft:bamboo_chest_raft", V4067::chestBoat);
    }

    static @NotNull Field chestBoat(@NotNull Field field) {
        return field
                .list("Items", DataType.ITEM_STACK);
    }

}
