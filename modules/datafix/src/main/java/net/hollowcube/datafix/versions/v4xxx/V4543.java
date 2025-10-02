package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4543 extends DataVersion {

    public V4543() {
        super(4533);

        addReference(DataTypes.ENTITY, "minecraft:mannequin");
    }

}
