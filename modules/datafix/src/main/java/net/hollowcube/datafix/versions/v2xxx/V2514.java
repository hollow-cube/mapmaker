package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.UUIDFixes;
import net.hollowcube.datafix.util.Value;

import java.util.Set;

public class V2514 extends DataVersion {
    private static final Set<String> ABSTRACT_HORSES;
    private static final Set<String> TAMEABLE_ANIMALS;
    private static final Set<String> ANIMALS;
    private static final Set<String> MOBS;
    private static final Set<String> LIVING_ENTITIES;
    private static final Set<String> PROJECTILES;

    public V2514() {
        super(2514);

        addFix(DataTypes.ENTITY, V2514::updateEntityUuid);
        ABSTRACT_HORSES.forEach(id -> addFix(DataTypes.ENTITY, id, V2514::updateAnimalOwner));
        TAMEABLE_ANIMALS.forEach(id -> addFix(DataTypes.ENTITY, id, V2514::updateAnimalOwner));
        ANIMALS.forEach(id -> addFix(DataTypes.ENTITY, id, V2514::updateAnimal));
        MOBS.forEach(id -> addFix(DataTypes.ENTITY, id, V2514::updateMob));
        LIVING_ENTITIES.forEach(id -> addFix(DataTypes.ENTITY, id, V2514::updateLivingEntity));
        PROJECTILES.forEach(id -> addFix(DataTypes.ENTITY, id, V2514::updateProjectile));
        addFix(DataTypes.ENTITY, "minecraft:bee", V2514::updateHurtBy);
        addFix(DataTypes.ENTITY, "minecraft:zombified_piglin", V2514::updateHurtBy);
        addFix(DataTypes.ENTITY, "minecraft:fox", V2514::updateFox);
        addFix(DataTypes.ENTITY, "minecraft:item", V2514::updateItem);
        addFix(DataTypes.ENTITY, "minecraft:shulker_bullet", V2514::updateShulkerBullet);
        addFix(DataTypes.ENTITY, "minecraft:area_effect_cloud", V2514::updateAreaEffectCloud);
        addFix(DataTypes.ENTITY, "minecraft:zombie_villager", V2514::updateZombieVillager);
        addFix(DataTypes.ENTITY, "minecraft:evoker_fangs", V2514::updateEvokerFangs);
        addFix(DataTypes.ENTITY, "minecraft:piglin", V2514::updatePiglin);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:conduit", V2514::updateConduitBlockEntity);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:skull", V2514::updateSkullBlockEntity);

        addFix(DataTypes.ITEM_STACK, V2514::updateItemAttributeModifiers);
        addFix(DataTypes.ITEM_STACK, "minecraft:player_head", V2514::updateSkullItem);
    }

    private static Value updateEntityUuid(Value entity) {
        return UUIDFixes.replaceUuidFromLeastMost(entity, "UUID", "UUID");
    }

    private static Value updateAnimalOwner(Value entity) {
        var _ = updateAnimal(entity);
        return UUIDFixes.replaceUuidFromLeastMost(entity, "OwnerUUID", "Owner");
    }

    private static Value updateAnimal(Value entity) {
        var _ = updateMob(entity);
        return UUIDFixes.replaceUuidFromLeastMost(entity, "LoveCause", "LoveCause");
    }

    private static Value updateMob(Value entity) {
        var _ = updateLivingEntity(entity);
        return UUIDFixes.replaceUuidFromLeastMost(entity.get("Leash"), "UUID", "UUID");
    }

    private static Value updateLivingEntity(Value entity) {
        for (var attribute : entity.get("Attributes")) {
            for (var modifier : attribute.get("Modifiers")) {
                UUIDFixes.replaceUuidFromLeastMost(modifier, "UUID", "UUID");
            }
        }
        return null;
    }

    private static Value updateProjectile(Value entity) {
        entity.put("Owner", entity.remove("OwnerUUID"));
        return null;
    }

    private static Value updateHurtBy(Value entity) {
        return UUIDFixes.replaceUuidFromLeastMost(entity, "HurtBy", "HurtBy");
    }

    private static Value updateFox(Value entity) {
        var trustedUuids = entity.remove("TrustedUUIDs");
        if (trustedUuids.size(0) == 0) return null;

        var newTrustedUuids = Value.emptyList();
        for (var trustedUuid : trustedUuids) {
            var mostSignificantBits = trustedUuid.remove("M").as(Number.class, 0L).longValue();
            var leastSignificantBits = trustedUuid.remove("L").as(Number.class, 0L).longValue();
            newTrustedUuids.put(UUIDFixes.createUuidArray(mostSignificantBits, leastSignificantBits));
        }
        entity.put("Trusted", newTrustedUuids);
        return null;
    }

    private static Value updateItem(Value entity) {
        var _ = UUIDFixes.replaceUuidFromMLTag(entity, "Owner", "Owner");
        return UUIDFixes.replaceUuidFromMLTag(entity, "Thrower", "Thrower");
    }

    private static Value updateShulkerBullet(Value entity) {
        var _ = UUIDFixes.replaceUuidFromMLTag(entity, "Owner", "Owner");
        return UUIDFixes.replaceUuidFromMLTag(entity, "Target", "Target");
    }

    private static Value updateAreaEffectCloud(Value entity) {
        return UUIDFixes.replaceUuidFromMLTag(entity, "OwnerUUID", "Owner");
    }

    private static Value updateZombieVillager(Value entity) {
        return UUIDFixes.replaceUuidFromMLTag(entity, "ConversionPlayer", "ConversionPlayer");
    }

    private static Value updateEvokerFangs(Value entity) {
        return UUIDFixes.replaceUuidFromMLTag(entity, "OwnerUUID", "Owner");
    }

    private static Value updatePiglin(Value entity) {
        var memories = entity.get("Brain").get("memories");
        var angryAt = memories.get("minecraft:angry_at");
        if (angryAt.isMapLike()) UUIDFixes.replaceUuidFromString(angryAt, "value", "value");
        return null;
    }

    private static Value updateConduitBlockEntity(Value blockEntity) {
        return UUIDFixes.replaceUuidFromMLTag(blockEntity, "target_uuid", "Target");
    }

    private static Value updateSkullBlockEntity(Value blockEntity) {
        UUIDFixes.replaceUuidFromString(blockEntity.get("Owner"), "Id", "Id");
        blockEntity.put("SkullOwner", blockEntity.remove("Owner"));
        return null;
    }

    private static Value updateItemAttributeModifiers(Value blockEntity) {
        var tag = blockEntity.get("tag");
        if (!tag.isMapLike()) return null;

        for (var modifier : tag.get("AttributeModifiers")) {
            UUIDFixes.replaceUuidFromLeastMost(modifier, "UUID", "UUID");
        }

        return null;
    }

    private static Value updateSkullItem(Value blockEntity) {
        var tag = blockEntity.get("tag");
        if (!tag.isMapLike()) return null;

        return UUIDFixes.replaceUuidFromString(tag.get("SkullOwner"), "Id", "Id");
    }

    static {
        ABSTRACT_HORSES = Set.of(
                "minecraft:donkey",
                "minecraft:horse",
                "minecraft:llama",
                "minecraft:mule",
                "minecraft:skeleton_horse",
                "minecraft:trader_llama",
                "minecraft:zombie_horse"
        );
        TAMEABLE_ANIMALS = Set.of(
                "minecraft:cat",
                "minecraft:parrot",
                "minecraft:wolf"
        );
        ANIMALS = Set.of(
                "minecraft:bee",
                "minecraft:chicken",
                "minecraft:cow",
                "minecraft:fox",
                "minecraft:mooshroom",
                "minecraft:ocelot",
                "minecraft:panda",
                "minecraft:pig",
                "minecraft:polar_bear",
                "minecraft:rabbit",
                "minecraft:sheep",
                "minecraft:turtle",
                "minecraft:hoglin"
        );
        MOBS = Set.of(
                "minecraft:bat",
                "minecraft:blaze",
                "minecraft:cave_spider",
                "minecraft:cod",
                "minecraft:creeper",
                "minecraft:dolphin",
                "minecraft:drowned",
                "minecraft:elder_guardian",
                "minecraft:ender_dragon",
                "minecraft:enderman",
                "minecraft:endermite",
                "minecraft:evoker",
                "minecraft:ghast",
                "minecraft:giant",
                "minecraft:guardian",
                "minecraft:husk",
                "minecraft:illusioner",
                "minecraft:magma_cube",
                "minecraft:pufferfish",
                "minecraft:zombified_piglin",
                "minecraft:salmon",
                "minecraft:shulker",
                "minecraft:silverfish",
                "minecraft:skeleton",
                "minecraft:slime",
                "minecraft:snow_golem",
                "minecraft:spider",
                "minecraft:squid",
                "minecraft:stray",
                "minecraft:tropical_fish",
                "minecraft:vex",
                "minecraft:villager",
                "minecraft:iron_golem",
                "minecraft:vindicator",
                "minecraft:pillager",
                "minecraft:wandering_trader",
                "minecraft:witch",
                "minecraft:wither",
                "minecraft:wither_skeleton",
                "minecraft:zombie",
                "minecraft:zombie_villager",
                "minecraft:phantom",
                "minecraft:ravager",
                "minecraft:piglin"
        );
        LIVING_ENTITIES = Set.of("minecraft:armor_stand");
        PROJECTILES = Set.of(
                "minecraft:arrow",
                "minecraft:dragon_fireball",
                "minecraft:firework_rocket",
                "minecraft:fireball",
                "minecraft:llama_spit",
                "minecraft:small_fireball",
                "minecraft:snowball",
                "minecraft:spectral_arrow",
                "minecraft:egg",
                "minecraft:ender_pearl",
                "minecraft:experience_bottle",
                "minecraft:potion",
                "minecraft:trident",
                "minecraft:wither_skull"
        );
    }
}
