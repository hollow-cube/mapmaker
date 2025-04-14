package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.Set;

public class V109 extends DataVersion {
    private static final Set<String> ENTITIES;

    public V109() {
        super(109);

        addFix(DataTypes.ENTITY, V109::fixEntityHealth);
    }

    private static Value fixEntityHealth(Value entity) {
        if (entity.getValue("HealF") instanceof Number n) {
            entity.put("HealF", null);
            entity.put("Health", n.floatValue());
        } else if (entity.getValue("Health") instanceof Number n) {
            entity.put("Health", n.floatValue());
        }
        return null;
    }

    static {
        ENTITIES = Set.of(
                "ArmorStand",
                "Bat",
                "Blaze",
                "CaveSpider",
                "Chicken",
                "Cow",
                "Creeper",
                "EnderDragon",
                "Enderman",
                "Endermite",
                "EntityHorse",
                "Ghast",
                "Giant",
                "Guardian",
                "LavaSlime",
                "MushroomCow",
                "Ozelot",
                "Pig",
                "PigZombie",
                "Rabbit",
                "Sheep",
                "Shulker",
                "Silverfish",
                "Skeleton",
                "Slime",
                "SnowMan",
                "Spider",
                "Squid",
                "Villager",
                "VillagerGolem",
                "Witch",
                "WitherBoss",
                "Wolf",
                "Zombie"
        );
    }
}
