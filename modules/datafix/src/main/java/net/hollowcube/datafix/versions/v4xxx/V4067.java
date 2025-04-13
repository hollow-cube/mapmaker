package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
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

        addFix(DataType.ENTITY, "minecraft:boat", V4067::fixBoatSplit);
        addFix(DataType.ENTITY, "minecraft:chest_boat", V4067::fixChestBoatSplit);
    }

    static @NotNull Field chestBoat(@NotNull Field field) {
        return field.list("Items", DataType.ITEM_STACK);
    }

    private static Value fixBoatSplit(Value entity) {
        entity.put("id", getBoatType(entity));
        return null;
    }

    private static Value fixChestBoatSplit(Value entity) {
        entity.put("id", getBoatType(entity).replace("_", "_chest_"));
        return null;
    }

    private static String getBoatType(Value entity) {
        return switch (entity.remove("Type").as(String.class, "oak")) {
            case "spruce" -> "minecraft:spruce_boat";
            case "birch" -> "minecraft:birch_boat";
            case "jungle" -> "minecraft:jungle_boat";
            case "acacia" -> "minecraft:acacia_boat";
            case "cherry" -> "minecraft:cherry_boat";
            case "dark_oak" -> "minecraft:dark_oak_boat";
            case "mangrove" -> "minecraft:mangrove_boat";
            case "bamboo" -> "minecraft:bamboo_raft";
            default -> "minecraft:oak_boat";
        };
    }

}
