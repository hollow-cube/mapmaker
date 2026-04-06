package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4763 extends DataVersion {

    public V4763() {
        super(4763);

        addFix(DataTypes.ENTITY, "minecraft:villager", V4763::addVillagerDataFinalized);
        addFix(DataTypes.ENTITY, "minecraft:zombie_villager", V4763::addVillagerDataFinalized);
    }

    private static Value addVillagerDataFinalized(Value entity) {
        entity.put("VillagerDataFinalized", true);
        return null;
    }

}
