package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.util.Value;

import java.util.concurrent.ThreadLocalRandom;

public class V502 extends DataVersion {
    private static final int PROFESSION_MAX = 6;

    public V502() {
        super(502);

        addFix(DataTypes.ITEM_NAME, new ItemRenameFix("minecraft:cooked_fished", "minecraft:cooked_fish"));
        addFix(DataTypes.ENTITY, "Zombie", V502::fixZombieVillagerType);
    }

    private static Value fixZombieVillagerType(Value value) {
        if (!value.get("IsVillager").as(Boolean.class, false))
            return null;
        value.put("IsVillager", null);

        if (value.getValue("ZombieType") == null)
            return null;

        int type = java.lang.Math.min(value.get("VillagerProfession").as(Number.class, -1).intValue(), 6);
        if (type < 0) type = ThreadLocalRandom.current().nextInt(6);

        value.put("ZombieType", type);
        return null;
    }
}
