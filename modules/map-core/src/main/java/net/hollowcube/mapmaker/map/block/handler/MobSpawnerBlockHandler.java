package net.hollowcube.mapmaker.map.block.handler;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MobSpawnerBlockHandler implements BlockHandler {

    public static final NamespaceID ID = NamespaceID.from("minecraft:mob_spawner");

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public boolean onInteract(@NotNull BlockHandler.Interaction interaction) {
        System.out.println("MOB SPAWNER INTERACTIONS");
        ItemStack itemStack = interaction.getPlayer().getItemInHand(interaction.getHand());
        EntityType entityType = itemStack.material().registry().spawnEntityType();
        if (entityType == null) return false;

        String entityId = entityType.namespace().asString();
        UUID uuid = UUID.randomUUID();
        // Convert to 4 ints
        long mostSig = uuid.getMostSignificantBits();
        long leastSig = uuid.getLeastSignificantBits();
        int mostSigA = (int) (mostSig >> 32);
        int mostSigB = (int) mostSig;
        int leastSigA = (int) (leastSig >> 32);
        int leastSigB = (int) leastSig;

        Block newBlock = interaction.getBlock().withTag(SPAWN_DATA, new EntityData(
                (short) 300, Component.empty(), false, 0f, (short) 0, false, false, entityId, false, List.of(0d, 0d, 0d), false, true, List.of(),
                0, List.of(0d, 0d, 0d), List.of(0f, 0f), false, List.of(), 0, List.of(mostSigA, mostSigB, leastSigA, leastSigB)
        )).withTag(DELAY, (short) 100).withTag(MAX_NEARBY_ENTITIES, (short) 6).withTag(MAX_SPAWN_DELAY, (short) 800).withTag(MIN_SPAWN_DELAY, (short) 200).withTag(REQUIRED_PLAYER_RANGE, (short) 16).withTag(SPAWN_COUNT, (short) 4);

        interaction.getInstance().setBlock(interaction.getBlockPosition(), newBlock);
        System.out.println("SET ENTITY");

        return true;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(DELAY, MAX_NEARBY_ENTITIES, MAX_SPAWN_DELAY, MIN_SPAWN_DELAY, REQUIRED_PLAYER_RANGE, SPAWN_COUNT, SPAWN_DATA, SPAWN_RANGE);
    }

    private static final Tag<Short> DELAY = Tag.Short("Delay").defaultValue( (short) 500);
    private static final Tag<Short> MAX_NEARBY_ENTITIES = Tag.Short("MaxNearbyEntities").defaultValue( (short) 6);
    private static final Tag<Short> MAX_SPAWN_DELAY = Tag.Short("MaxSpawnDelay").defaultValue( (short) 800);
    private static final Tag<Short> MIN_SPAWN_DELAY = Tag.Short("MinSpawnDelay").defaultValue((short) 200);
    private static final Tag<Short> REQUIRED_PLAYER_RANGE = Tag.Short("RequiredPlayerRange").defaultValue((short) 16);
    private static final Tag<Short> SPAWN_COUNT = Tag.Short("SpawnCount").defaultValue((short) 4);
    private static final Tag<EntityData> SPAWN_DATA = Tag.Structure("SpawnData", new EntitySerializer());
    private static final Tag<List<EntitySpawnList>> SPAWN_POTENTIALS = Tag.Structure("SpawnPotentials", new EntitySpawnListSerializer()).list();
    private static final Tag<Short> SPAWN_RANGE = Tag.Short("SpawnRange").defaultValue((short) 4);

    private record EntityData(short air, @NotNull Component customName, boolean customNameVisible, float fallDistance, short fireTicks, boolean hasGlowing,
                              boolean hasVisualFire, String id, boolean invunlerable, List<Double> motion, boolean noGravity, boolean onGround, List<EntityData> passengers,
                              int portalCooldown, List<Double> pos, List<Float> rotation, boolean silent, List<NBT> scoreboardTags, int ticksFrozen, List<Integer> uuid) {}

    private static final class EntitySerializer implements TagSerializer<EntityData> {
        private final Tag<Short> AIR_TICKS = Tag.Short("Air").defaultValue((short) 300);
        private final Tag<Component> CUSTOM_NAME = Tag.Component("CustomName").defaultValue(Component.empty());
        private final Tag<Boolean> CUSTOM_NAME_VISIBLE = Tag.Boolean("CustomNameVisible").defaultValue(false);
        private final Tag<Float> FALL_DISTANCE = Tag.Float("FallDistance").defaultValue(0f);
        private final Tag<Short> FIRE_TICKS = Tag.Short("Fire").defaultValue((short) 0);
        private final Tag<Boolean> HAS_GLOWING = Tag.Boolean("Glowing").defaultValue(false);
        private final Tag<Boolean> HAS_VISUAL_FIRE = Tag.Boolean("HasVisualFire").defaultValue(false);
        private final Tag<String> ID = Tag.String("id").defaultValue("allay");
        private final Tag<Boolean> INVULNERABLE = Tag.Boolean("Invulnerable").defaultValue(false);
        private final Tag<List<Double>> MOTION = Tag.Double("Motion").list();
        private final Tag<Boolean> NO_GRAVITY = Tag.Boolean("NoGravity").defaultValue(false);
        private final Tag<Boolean> ON_GROUND = Tag.Boolean("OnGround").defaultValue(false);
        //private final Tag<List<EntityData>> PASSENGERS = Tag.Structure("Passengers", new EntitySerializer()).list();
        private final Tag<Integer> PORTAL_COOLDOWN = Tag.Integer("PortalCooldown").defaultValue(0);
        private final Tag<List<Double>> POSITION = Tag.Double("Pos").list();
        private final Tag<List<Float>> ROTATION = Tag.Float("Rotation").list();
        private final Tag<Boolean> IS_SILENT = Tag.Boolean("Silent");
        private final Tag<List<NBT>> SCOREBOARD_TAGS = Tag.NBT("Tags").list();
        private final Tag<Integer> FROZEN_TICKS = Tag.Integer("TicksFrozen");
        private final Tag<List<Integer>> UUID = Tag.Integer("UUID").list();

        @Override
        public @NotNull EntityData read(@NotNull TagReadable reader) {
            short air = reader.getTag(AIR_TICKS);
            Component name = reader.getTag(CUSTOM_NAME);
            boolean nameVisible = reader.getTag(CUSTOM_NAME_VISIBLE);
            float fallDistance = reader.getTag(FALL_DISTANCE);
            short fireTicks = reader.getTag(FIRE_TICKS);
            boolean glowing = reader.getTag(HAS_GLOWING);
            boolean hasVisualFire = reader.getTag(HAS_VISUAL_FIRE);
            String id = reader.getTag(ID);
            boolean invulnerable = reader.getTag(INVULNERABLE);
            List<Double> motion = reader.getTag(MOTION);
            boolean noGravity = reader.getTag(NO_GRAVITY);
            boolean onGround = reader.getTag(ON_GROUND);
            //List<EntityData> passengers = reader.getTag(PASSENGERS);
            int portalCooldown = reader.getTag(PORTAL_COOLDOWN);
            List<Double> pos = reader.getTag(POSITION);
            List<Float> rotation = reader.getTag(ROTATION);
            boolean silent = reader.getTag(IS_SILENT);
            List<NBT> scoreboardTags = reader.getTag(SCOREBOARD_TAGS);
            int frozenTicks = reader.getTag(FROZEN_TICKS);
            List<Integer> uuid = reader.getTag(UUID);

            return new EntityData(air, name, nameVisible, fallDistance, fireTicks, glowing, hasVisualFire, id, invulnerable, motion, noGravity, onGround, null, portalCooldown, pos, rotation, silent, scoreboardTags, frozenTicks, uuid);
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull EntityData value) {
            writer.setTag(AIR_TICKS, value.air);
            writer.setTag(CUSTOM_NAME, value.customName);
            writer.setTag(CUSTOM_NAME_VISIBLE, value.customNameVisible);
            writer.setTag(FALL_DISTANCE, value.fallDistance);
            writer.setTag(FIRE_TICKS, value.fireTicks);
            writer.setTag(HAS_GLOWING, value.hasGlowing);
            writer.setTag(HAS_VISUAL_FIRE, value.hasVisualFire);
            writer.setTag(ID, value.id);
            writer.setTag(INVULNERABLE, value.invunlerable);
            writer.setTag(MOTION, value.motion);
            writer.setTag(NO_GRAVITY, value.noGravity);
            writer.setTag(ON_GROUND, value.onGround);
            //writer.setTag(PASSENGERS, value.passengers);
            writer.setTag(PORTAL_COOLDOWN, value.portalCooldown);
            writer.setTag(POSITION, value.pos);
            writer.setTag(ROTATION, value.rotation);
            writer.setTag(IS_SILENT, value.silent);
            writer.setTag(SCOREBOARD_TAGS, value.scoreboardTags);
            writer.setTag(FROZEN_TICKS, value.ticksFrozen);
            writer.setTag(UUID, value.uuid);
        }
    }

    private record CustomSpawnRules(int block_light_limit, int sky_light_limit) {}

    private static final class SpawnRulesSerializer implements TagSerializer<CustomSpawnRules> {
        private final Tag<Integer> BLOCK_LIGHT_LIMIT = Tag.Integer("block_light_limit");
        private final Tag<Integer> SKY_LIGHT_LIMIT = Tag.Integer("sky_light_limit");

        @Override
        public @NotNull CustomSpawnRules read(@NotNull TagReadable reader) {
            int blockLight = reader.getTag(BLOCK_LIGHT_LIMIT);
            int skyLight = reader.getTag(SKY_LIGHT_LIMIT);
            return new CustomSpawnRules(blockLight, skyLight);
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull CustomSpawnRules value) {
            writer.setTag(BLOCK_LIGHT_LIMIT, value.block_light_limit);
            writer.setTag(SKY_LIGHT_LIMIT, value.sky_light_limit);
        }
    }

    private record EntitySpawnData(@NotNull EntityData entity, @NotNull CustomSpawnRules custom_spawn_rules) {}

    private static class SpawnDataSerializer implements TagSerializer<EntitySpawnData> {
        private final Tag<EntityData> ENTITY_DATA = Tag.Structure("entity", new EntitySerializer());
        private final Tag<CustomSpawnRules> CUSTOM_SPAWN_LIGHT = Tag.Structure("custom_spawn_rules", new SpawnRulesSerializer());

        @Override
        public @NotNull EntitySpawnData read(@NotNull TagReadable reader) {
            EntityData entityData = reader.getTag(ENTITY_DATA);
            CustomSpawnRules customSpawnRules = reader.getTag(CUSTOM_SPAWN_LIGHT);

            return new EntitySpawnData(entityData, customSpawnRules);
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull EntitySpawnData value) {
            writer.setTag(ENTITY_DATA, value.entity);
            writer.setTag(CUSTOM_SPAWN_LIGHT, value.custom_spawn_rules);
        }
    }

    private record EntitySpawnList(int weight, @NotNull EntitySpawnData data) {}

    private static class EntitySpawnListSerializer implements TagSerializer<EntitySpawnList> {
        private final Tag<Integer> WEIGHT = Tag.Integer("weight");
        private final Tag<EntitySpawnData> DATA = Tag.Structure("data", new SpawnDataSerializer());

        @Override
        public @NotNull EntitySpawnList read(@NotNull TagReadable reader) {
            int weight = reader.getTag(WEIGHT);
            EntitySpawnData entitySpawnData = reader.getTag(DATA);

            return new EntitySpawnList(weight, entitySpawnData);
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull EntitySpawnList value) {
            writer.setTag(WEIGHT, value.weight);
            writer.setTag(DATA, value.data);
        }
    }
}
