package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2522 extends DataVersion {
    public V2522() {
        super(2522);

        addReference(DataType.ENTITY, "minecraft:zoglin");
    }
}
