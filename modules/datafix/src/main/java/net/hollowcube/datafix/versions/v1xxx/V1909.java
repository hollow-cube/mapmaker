package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1909 extends DataVersion {
    public V1909() {
        super(1909);

        addReference(DataType.BLOCK_ENTITY, "minecraft:jigsaw", field -> field
                .single("final_state", DataType.FLAT_BLOCK_STATE));
    }
}
