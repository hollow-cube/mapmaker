package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class V3945 extends DataVersion {
    private static final Map<UUID, String> ATTRIBUTE_MODIFIER_RENAMES;
    private static final Map<String, String> NAME_MAP;

    public V3945() {
        super(3945);

        addFix(DataTypes.ENTITY, V3945::fixAttributesForEntity);
        addFix(DataTypes.DATA_COMPONENTS, V3945::fixAttributesForComponents);

        addFix(DataTypes.BLOCK_ENTITY, V3945::fixJukeboxTicksSinceStarted);
    }

    private static Value fixJukeboxTicksSinceStarted(Value blockEntity) {
        long tickCount = blockEntity.remove("TickCount").as(Number.class, 0L).longValue();
        long recordStartTick = blockEntity.remove("RecordStartTick").as(Number.class, 0L).longValue();
        blockEntity.remove("IsPlaying");
        long ticksSinceStarted = tickCount - recordStartTick;
        if (ticksSinceStarted > 0) blockEntity.put("ticks_since_song_started", ticksSinceStarted);
        return null;
    }

    private static Value fixAttributesForEntity(Value entity) {
        entity.put("attributes", entity.remove("Attributes"));
        for (var attribute : entity.get("attributes")) {
            attribute.put("id", attribute.remove("Name"));
            attribute.put("base", attribute.remove("Base"));

            var modifiers = attribute.remove("Modifiers");
            for (var modifier : modifiers) {
                modifier.put("uuid", modifier.remove("UUID"));
                modifier.put("name", modifier.remove("Name"));
                modifier.put("amount", modifier.remove("Amount"));
                int operation = modifier.remove("Operation").as(Number.class, 0).intValue();
                modifier.put("operation", switch (operation) {
                    case 0 -> "add_value";
                    case 1 -> "add_multiplied_base";
                    case 2 -> "add_multiplied_total";
                    default -> "invalid";
                });

            }
            attribute.put("modifiers", fixModifiers(modifiers));
        }
        return null;
    }

    private static Value fixAttributesForComponents(Value components) {
        var modifiers = components.get("minecraft:attribute_modifiers");
        components.put("modifiers", fixModifiers(modifiers.remove("modifiers")));
        return null;
    }

    private static Value fixModifiers(Value modifiers) {
        var cachedModifiers = new HashMap<String, Value>();
        var newModifiers = Value.emptyList();
        for (var modifier : modifiers) {
            var uuid = uuidFromIntArray(modifier.remove("uuid").as(int[].class, null));
            var newIdFromUuid = uuid == null ? null : ATTRIBUTE_MODIFIER_RENAMES.get(uuid);

            var name = modifier.remove("name").as(String.class, null);
            var newIdFromName = name == null ? null : NAME_MAP.get(name);

            var newId = newIdFromUuid != null ? newIdFromUuid : (newIdFromName != null
                    ? newIdFromName : ("minecraft:" + (uuid != null ? uuid.toString() : "unknown")));
            var cached = cachedModifiers.get(newId);
            if (cached != null) {
                var a = cached.get("amount").as(Number.class, 0.0).doubleValue();
                var b = modifier.get("amount").as(Number.class, 0.0).doubleValue();
                cached.put("amount", a + b);
            } else {
                modifier.put("id", newId);
                newModifiers.put(modifier);
                cachedModifiers.put(newId, modifier);
            }
        }
        return newModifiers.size(0) > 0 ? newModifiers : null;
    }

    public static @Nullable UUID uuidFromIntArray(int[] is) {
        return is == null || is.length != 4 ? null : new UUID(
                (long) is[0] << 32 | (long) is[1] & 4294967295L,
                (long) is[2] << 32 | (long) is[3] & 4294967295L);
    }

    static {
        ATTRIBUTE_MODIFIER_RENAMES = Map.ofEntries(
                Map.entry(UUID.fromString("736565d2-e1a7-403d-a3f8-1aeb3e302542"), "minecraft:creative_mode_block_range"),
                Map.entry(UUID.fromString("98491ef6-97b1-4584-ae82-71a8cc85cf73"), "minecraft:creative_mode_entity_range"),
                Map.entry(UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635"), "minecraft:effect.speed"),
                Map.entry(UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890"), "minecraft:effect.slowness"),
                Map.entry(UUID.fromString("AF8B6E3F-3328-4C0A-AA36-5BA2BB9DBEF3"), "minecraft:effect.haste"),
                Map.entry(UUID.fromString("55FCED67-E92A-486E-9800-B47F202C4386"), "minecraft:effect.mining_fatigue"),
                Map.entry(UUID.fromString("648D7064-6A60-4F59-8ABE-C2C23A6DD7A9"), "minecraft:effect.strength"),
                Map.entry(UUID.fromString("C0105BF3-AEF8-46B0-9EBC-92943757CCBE"), "minecraft:effect.jump_boost"),
                Map.entry(UUID.fromString("22653B89-116E-49DC-9B6B-9971489B5BE5"), "minecraft:effect.weakness"),
                Map.entry(UUID.fromString("5D6F0BA2-1186-46AC-B896-C61C5CEE99CC"), "minecraft:effect.health_boost"),
                Map.entry(UUID.fromString("EAE29CF0-701E-4ED6-883A-96F798F3DAB5"), "minecraft:effect.absorption"),
                Map.entry(UUID.fromString("03C3C89D-7037-4B42-869F-B146BCB64D2E"), "minecraft:effect.luck"),
                Map.entry(UUID.fromString("CC5AF142-2BD2-4215-B636-2605AED11727"), "minecraft:effect.unluck"),
                Map.entry(UUID.fromString("6555be74-63b3-41f1-a245-77833b3c2562"), "minecraft:evil"),
                Map.entry(UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce"), "minecraft:powder_snow"),
                Map.entry(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "minecraft:sprinting"),
                Map.entry(UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0"), "minecraft:attacking"),
                Map.entry(UUID.fromString("766bfa64-11f3-11ea-8d71-362b9e155667"), "minecraft:baby"),
                Map.entry(UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF27F"), "minecraft:covered"),
                Map.entry(UUID.fromString("9e362924-01de-4ddd-a2b2-d0f7a405a174"), "minecraft:suffocating"),
                Map.entry(UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E"), "minecraft:drinking"),
                Map.entry(UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836"), "minecraft:baby"),
                Map.entry(UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718"), "minecraft:attacking"),
                Map.entry(UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), "minecraft:armor.boots"),
                Map.entry(UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), "minecraft:armor.leggings"),
                Map.entry(UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), "minecraft:armor.chestplate"),
                Map.entry(UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), "minecraft:armor.helmet"),
                Map.entry(UUID.fromString("C1C72771-8B8E-BA4A-ACE0-81A93C8928B2"), "minecraft:armor.body"),
                Map.entry(UUID.fromString("b572ecd2-ac0c-4071-abde-9594af072a37"), "minecraft:enchantment.fire_protection"),
                Map.entry(UUID.fromString("40a9968f-5c66-4e2f-b7f4-2ec2f4b3e450"), "minecraft:enchantment.blast_protection"),
                Map.entry(UUID.fromString("07a65791-f64d-4e79-86c7-f83932f007ec"), "minecraft:enchantment.respiration"),
                Map.entry(UUID.fromString("60b1b7db-fffd-4ad0-817c-d6c6a93d8a45"), "minecraft:enchantment.aqua_affinity"),
                Map.entry(UUID.fromString("11dc269a-4476-46c0-aff3-9e17d7eb6801"), "minecraft:enchantment.depth_strider"),
                Map.entry(UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038"), "minecraft:enchantment.soul_speed"),
                Map.entry(UUID.fromString("b9716dbd-50df-4080-850e-70347d24e687"), "minecraft:enchantment.soul_speed"),
                Map.entry(UUID.fromString("92437d00-c3a7-4f2e-8f6c-1f21585d5dd0"), "minecraft:enchantment.swift_sneak"),
                Map.entry(UUID.fromString("5d3d087b-debe-4037-b53e-d84f3ff51f17"), "minecraft:enchantment.sweeping_edge"),
                Map.entry(UUID.fromString("3ceb37c0-db62-46b5-bd02-785457b01d96"), "minecraft:enchantment.efficiency"),
                Map.entry(UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF"), "minecraft:base_attack_damage"),
                Map.entry(UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"), "minecraft:base_attack_speed")
        );
        NAME_MAP = Map.of(
                "Random spawn bonus", "minecraft:random_spawn_bonus",
                "Random zombie-spawn bonus", "minecraft:zombie_random_spawn_bonus",
                "Leader zombie bonus", "minecraft:leader_zombie_bonus",
                "Zombie reinforcement callee charge", "minecraft:reinforcement_callee_charge",
                "Zombie reinforcement caller charge", "minecraft:reinforcement_caller_charge"
        );
    }
}
