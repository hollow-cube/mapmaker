package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import org.jetbrains.annotations.NotNull;

public class V4300 extends DataVersion {
    public V4300() {
        super(4300);

        addReference(DataTypes.ENTITY, "minecraft:llama", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:trader_llama", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:donkey", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:mule", V4300::entityWithInventory);
        addReference(DataTypes.ENTITY, "minecraft:horse");
        addReference(DataTypes.ENTITY, "minecraft:skeleton_horse");
        addReference(DataTypes.ENTITY, "minecraft:zombie_horse");
    }

    static @NotNull DataType.Builder entityWithInventory(@NotNull DataType.Builder field) {
        return field
                .list("Items", DataTypes.ITEM_STACK);
    }

}
