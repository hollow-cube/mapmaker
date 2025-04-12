package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2509 extends DataVersion {
    public V2509() {
        super(2509);

        removeReference(DataType.ENTITY, "minecraft:zombie_pigman");
        addReference(DataType.ENTITY, "minecraft:zombified_piglin");
    }
}
