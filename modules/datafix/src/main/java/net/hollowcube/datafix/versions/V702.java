package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.entity.EntityRenameFix;
import net.hollowcube.datafix.util.Value;

public class V702 extends DataVersion {
    public V702() {
        super(702);

        addReference(DataType.ENTITY, "ZombieVillager");
        addReference(DataType.ENTITY, "Husk");

        addFix(DataType.ENTITY, "Zombie", new EntityRenameFix(V702::fixZombieSplit));
    }

    private static String fixZombieSplit(Value value, String s) {
        int zombieType = value.get("ZombieType").as(Number.class, 0).intValue();
        value.put("ZombieType", null);
        if (zombieType >= 1 && zombieType <= 5) {
            value.put("Profession", zombieType - 1);
            return "ZombieVillager";
        } else if (zombieType == 6) {
            return "Husk";
        } else return "Zombie";
    }
}
