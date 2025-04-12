package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2704 extends DataVersion {
    public V2704() {
        super(2704);

        addReference(DataType.ENTITY, "minecraft:goat");
    }
}
