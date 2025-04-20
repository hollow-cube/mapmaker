package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V1800 extends DataVersion {
    public static final Map<String, String> RENAMED_DYE_ITEMS = Map.of(
            "minecraft:cactus_green", "minecraft:green_dye",
            "minecraft:rose_red", "minecraft:red_dye",
            "minecraft:dandelion_yellow", "minecraft:yellow_dye"
    );

    public V1800() {
        super(1800);

        addReference(DataTypes.ENTITY, "minecraft:panda");
        addReference(DataTypes.ENTITY, "minecraft:pillager", field -> field
                .list("Inventory", DataTypes.ITEM_STACK));

        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(RENAMED_DYE_ITEMS));
    }
}
