package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3683 extends DataVersion {
    public V3683() {
        super(3683);

        addReference(DataType.ENTITY, "minecraft:tnt", field -> field
                .single("block_state", DataType.BLOCK_STATE));
    }
}
