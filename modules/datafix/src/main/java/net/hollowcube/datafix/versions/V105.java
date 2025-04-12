package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V105 extends DataVersion {
    private static final String[] ID_TO_ENTITY = new String[256];

    public V105() {
        super(105);

        addFix(DataType.ITEM_STACK, V105::fixSpawnEggEntityId);
    }

    private static Value fixSpawnEggEntityId(Value itemStack) {
        if (!"minecraft:spawn_egg".equals(itemStack.get("id").value()))
            return null;

        short damage = itemStack.get("Damage").as(Number.class, 0).shortValue();
        if (damage != 0) itemStack.put("Damage", (short) 0);

        String entityId = ID_TO_ENTITY[damage & 255];
        Object existingEntityId = itemStack.get("tag").get("EntityTag").value();
        if (entityId != null && !entityId.equals(existingEntityId)) {
            itemStack.get("tag", Value::emptyMap)
                    .get("EntityTag", Value::emptyMap)
                    .put("id", entityId);
        }

        return null;
    }

    static {
        ID_TO_ENTITY[1] = "Item";
        ID_TO_ENTITY[2] = "XPOrb";
        ID_TO_ENTITY[7] = "ThrownEgg";
        ID_TO_ENTITY[8] = "LeashKnot";
        ID_TO_ENTITY[9] = "Painting";
        ID_TO_ENTITY[10] = "Arrow";
        ID_TO_ENTITY[11] = "Snowball";
        ID_TO_ENTITY[12] = "Fireball";
        ID_TO_ENTITY[13] = "SmallFireball";
        ID_TO_ENTITY[14] = "ThrownEnderpearl";
        ID_TO_ENTITY[15] = "EyeOfEnderSignal";
        ID_TO_ENTITY[16] = "ThrownPotion";
        ID_TO_ENTITY[17] = "ThrownExpBottle";
        ID_TO_ENTITY[18] = "ItemFrame";
        ID_TO_ENTITY[19] = "WitherSkull";
        ID_TO_ENTITY[20] = "PrimedTnt";
        ID_TO_ENTITY[21] = "FallingSand";
        ID_TO_ENTITY[22] = "FireworksRocketEntity";
        ID_TO_ENTITY[23] = "TippedArrow";
        ID_TO_ENTITY[24] = "SpectralArrow";
        ID_TO_ENTITY[25] = "ShulkerBullet";
        ID_TO_ENTITY[26] = "DragonFireball";
        ID_TO_ENTITY[30] = "ArmorStand";
        ID_TO_ENTITY[41] = "Boat";
        ID_TO_ENTITY[42] = "MinecartRideable";
        ID_TO_ENTITY[43] = "MinecartChest";
        ID_TO_ENTITY[44] = "MinecartFurnace";
        ID_TO_ENTITY[45] = "MinecartTNT";
        ID_TO_ENTITY[46] = "MinecartHopper";
        ID_TO_ENTITY[47] = "MinecartSpawner";
        ID_TO_ENTITY[40] = "MinecartCommandBlock";
        ID_TO_ENTITY[50] = "Creeper";
        ID_TO_ENTITY[51] = "Skeleton";
        ID_TO_ENTITY[52] = "Spider";
        ID_TO_ENTITY[53] = "Giant";
        ID_TO_ENTITY[54] = "Zombie";
        ID_TO_ENTITY[55] = "Slime";
        ID_TO_ENTITY[56] = "Ghast";
        ID_TO_ENTITY[57] = "PigZombie";
        ID_TO_ENTITY[58] = "Enderman";
        ID_TO_ENTITY[59] = "CaveSpider";
        ID_TO_ENTITY[60] = "Silverfish";
        ID_TO_ENTITY[61] = "Blaze";
        ID_TO_ENTITY[62] = "LavaSlime";
        ID_TO_ENTITY[63] = "EnderDragon";
        ID_TO_ENTITY[64] = "WitherBoss";
        ID_TO_ENTITY[65] = "Bat";
        ID_TO_ENTITY[66] = "Witch";
        ID_TO_ENTITY[67] = "Endermite";
        ID_TO_ENTITY[68] = "Guardian";
        ID_TO_ENTITY[69] = "Shulker";
        ID_TO_ENTITY[90] = "Pig";
        ID_TO_ENTITY[91] = "Sheep";
        ID_TO_ENTITY[92] = "Cow";
        ID_TO_ENTITY[93] = "Chicken";
        ID_TO_ENTITY[94] = "Squid";
        ID_TO_ENTITY[95] = "Wolf";
        ID_TO_ENTITY[96] = "MushroomCow";
        ID_TO_ENTITY[97] = "SnowMan";
        ID_TO_ENTITY[98] = "Ozelot";
        ID_TO_ENTITY[99] = "VillagerGolem";
        ID_TO_ENTITY[100] = "EntityHorse";
        ID_TO_ENTITY[101] = "Rabbit";
        ID_TO_ENTITY[120] = "Villager";
        ID_TO_ENTITY[200] = "EnderCrystal";
    }
}
