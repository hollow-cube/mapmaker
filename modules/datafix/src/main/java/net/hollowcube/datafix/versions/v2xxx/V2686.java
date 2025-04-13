package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2686 extends DataVersion {
    public V2686() {
        super(2686);

        addReference(DataType.ENTITY, "minecraft:axolotl");
    }
}
