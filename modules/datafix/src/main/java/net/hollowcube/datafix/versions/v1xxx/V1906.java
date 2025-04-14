package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1906 extends DataVersion {
    public V1906() {
        super(1906);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:barrel", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:smoker", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:blast_furnace", V1458::nameableInventory);
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:lectern", field -> field
                .single("Book", DataTypes.ITEM_STACK));
        addReference(DataTypes.BLOCK_ENTITY, "minecraft:bell");
    }
}
