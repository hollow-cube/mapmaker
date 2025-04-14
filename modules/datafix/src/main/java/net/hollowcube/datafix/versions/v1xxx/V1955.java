package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1955 extends DataVersion {
    private static final int[] LEVEL_XP_THRESHOLDS = new int[]{0, 10, 50, 100, 150};

    public V1955() {
        super(1955);

        addFix(DataTypes.ENTITY, "minecraft:_villager", V1955::fixVillagerLevelAndXp);
        addFix(DataTypes.ENTITY, "minecraft:zombie_villager", V1955::fixZombieVillagerXp);
    }

    private static Value fixVillagerLevelAndXp(Value entity) {
        int level = entity.get("VillagerData").get("level").as(Number.class, 0).intValue();
        if (level == 0 || level == 1) {
            int tradeCount = entity.get("Offers").get("Recipes").size(0);
            level = Math.clamp(tradeCount / 2, 1, 5);
            if (level > 1) entity.get("VillagerData").put("level", level);
        }

        if (entity.get("Xp").isNull()) {
            entity.put("Xp", getMinXpPerLevel(level));
        }

        return null;
    }

    private static Value fixZombieVillagerXp(Value entity) {
        var xp = entity.get("Xp").as(Number.class, null);
        if (xp != null) {
            int level = entity.get("VillagerData").get("level").as(Number.class, 1).intValue();
            entity.put("Xp", getMinXpPerLevel(level));
        }
        return null;
    }

    private static int getMinXpPerLevel(int level) {
        return LEVEL_XP_THRESHOLDS[Math.clamp(level - 1, 0, LEVEL_XP_THRESHOLDS.length - 1)];
    }
}
