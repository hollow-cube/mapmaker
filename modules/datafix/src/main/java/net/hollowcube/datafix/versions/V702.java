package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V702 extends DataVersion {
    public V702() {
        super(702);

        addReference(DataType.ENTITY, "ZombieVillager");
        addReference(DataType.ENTITY, "Husk");
    }
}
