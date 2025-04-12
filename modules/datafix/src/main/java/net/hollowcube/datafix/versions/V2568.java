package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2568 extends DataVersion {
    public V2568() {
        super(2568);

        addReference(DataType.ENTITY, "minecraft:piglin_brute");
    }
}
