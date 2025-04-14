package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1451_2 extends DataVersion {
    public V1451_2() {
        super(1451, 2);

        addReference(DataTypes.BLOCK_ENTITY, "minecraft:piston", field -> field
                .single("blockState", DataTypes.BLOCK_STATE));

        // TODO: this also uses the gross massive block state upgrader
    }
}
