package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V101 extends DataVersion {

    public V101() {
        super(101);

        addFix(DataTypes.ENTITY, "Villager", V101::fixCanPickUpLoot);
    }

    private static Value fixCanPickUpLoot(Value entity) {
        entity.put("CanPickUpLoot", true);
        return entity;
    }
}
