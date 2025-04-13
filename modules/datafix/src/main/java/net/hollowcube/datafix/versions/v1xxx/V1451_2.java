package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1451_2 extends DataVersion {
    public V1451_2() {
        super(1451); // todo what is this version?

        addReference(DataType.BLOCK_ENTITY, "minecraft:piston", field -> field
                .single("blockState", DataType.BLOCK_STATE));

        // TODO: this also uses the gross massive block state upgrader
    }
}
