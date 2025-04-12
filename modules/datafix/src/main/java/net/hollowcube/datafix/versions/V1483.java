package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1483 extends DataVersion {
    public V1483() {
        super(1483);

        renameReference(DataType.ENTITY, "minecraft:puffer_fish", "minecraft:pufferfish");
    }
}
