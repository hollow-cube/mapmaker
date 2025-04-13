package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3807 extends DataVersion {
    public V3807() {
        super(3807);

        addReference(DataType.BLOCK_ENTITY, "minecraft:vault", field -> field
                .single("config.key_item", DataType.ITEM_STACK)
                .list("server_data.items_to_eject", DataType.ITEM_STACK)
                .single("server_data.display_item", DataType.ITEM_STACK));
    }

}
