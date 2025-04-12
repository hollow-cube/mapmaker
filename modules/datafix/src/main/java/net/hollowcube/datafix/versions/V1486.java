package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1486 extends DataVersion {
    public V1486() {
        super(1486);

        renameReference(DataType.ENTITY, "minecraft:cod_mob", "minecraft:cod");
        renameReference(DataType.ENTITY, "minecraft:salmon_mob", "minecraft:salmon");
    }
}
