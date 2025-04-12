package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1906 extends DataVersion {
    public V1906() {
        super(1906);

        addReference(DataType.BLOCK_ENTITY, "minecraft:barrel", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:smoker", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:blast_furnace", V1458::nameableInventory);
        addReference(DataType.BLOCK_ENTITY, "minecraft:lectern", field -> field
                .single("Book", DataType.ITEM_STACK));
        addReference(DataType.BLOCK_ENTITY, "minecraft:bell");
    }
}
