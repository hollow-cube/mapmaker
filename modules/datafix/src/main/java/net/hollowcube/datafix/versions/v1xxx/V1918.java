package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1918 extends DataVersion {
    public V1918() {
        super(1918);

        addFix(DataTypes.ENTITY, "minecraft:villager", V1918::fixVillagerData);
        addFix(DataTypes.ENTITY, "minecraft:zombie_villager", V1918::fixVillagerData);
    }

    private static Value fixVillagerData(Value value) {
        int profession = value.get("Profession").as(Number.class, 0).intValue();
        int career = value.get("Career").as(Number.class, 0).intValue();
        int careerLevel = value.get("CareerLevel").as(Number.class, 1).intValue();

        value.put("Profession", null);
        value.put("Career", null);
        value.put("CareerLevel", null);

        var villagerData = Value.emptyMap();
        villagerData.put("type", "minecraft:plains");
        villagerData.put("profession", upgradeVillagerType(profession, career));
        villagerData.put("level", careerLevel);
        value.put("VillagerData", villagerData);

        return null;
    }

    private static String upgradeVillagerType(int profession, int career) {
        return switch (profession) {
            case 0 -> switch (career) {
                case 2 -> "minecraft:fisherman";
                case 3 -> "minecraft:shepherd";
                case 4 -> "minecraft:fletcher";
                default -> "minecraft:farmer";
            };
            case 1 -> career == 2 ? "minecraft:cartographer" : "minecraft:librarian";
            case 2 -> "minecraft:cleric";
            case 3 -> switch (career) {
                case 2 -> "minecraft:weaponsmith";
                case 3 -> "minecraft:toolsmith";
                default -> "minecraft:armorer";
            };
            case 4 -> career == 2 ? "minecraft:leatherworker" : "minecraft:butcher";
            case 5 -> "minecraft:nitwit";
            default -> "minecraft:none";
        };
    }
}
