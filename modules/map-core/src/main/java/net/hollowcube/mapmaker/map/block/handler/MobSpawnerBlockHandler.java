package net.hollowcube.mapmaker.map.block.handler;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MobSpawnerBlockHandler implements BlockHandler {

    public static final Key ID = Key.key("minecraft:mob_spawner");

    @Override
    public Key getKey() {
        return ID;
    }

    @Override
    public boolean onInteract(BlockHandler.Interaction interaction) {
        if (!BlockHandlerHelpers.canEdit(interaction)) return true;

        ItemStack itemStack = interaction.getPlayer().getItemInHand(interaction.getHand());
        EntityType entityType = itemStack.material().registry().spawnEntityType();
        if (entityType == null) return true;

        String entityId = entityType.key().asString();

        Block newBlock = interaction.getBlock()
            .withTag(SPAWN_DATA, new EntityData(entityId))
            .withTag(DELAY, (short) 100)
            .withTag(MAX_NEARBY_ENTITIES, (short) 6)
            .withTag(MAX_SPAWN_DELAY, (short) 800)
            .withTag(MIN_SPAWN_DELAY, (short) 200)
            .withTag(REQUIRED_PLAYER_RANGE, (short) 16)
            .withTag(SPAWN_COUNT, (short) 4);

        interaction.getInstance().setBlock(interaction.getBlockPosition(), newBlock);

        return false;
    }

    @Override
    public Collection<Tag<?>> getBlockEntityTags() {
        return List.of(DELAY, MAX_NEARBY_ENTITIES, MAX_SPAWN_DELAY, MIN_SPAWN_DELAY, REQUIRED_PLAYER_RANGE, SPAWN_COUNT, SPAWN_DATA, SPAWN_RANGE);
    }

    private static final Tag<Short> DELAY = Tag.Short("Delay").defaultValue((short) 500);
    private static final Tag<Short> MAX_NEARBY_ENTITIES = Tag.Short("MaxNearbyEntities").defaultValue((short) 6);
    private static final Tag<Short> MAX_SPAWN_DELAY = Tag.Short("MaxSpawnDelay").defaultValue((short) 800);
    private static final Tag<Short> MIN_SPAWN_DELAY = Tag.Short("MinSpawnDelay").defaultValue((short) 200);
    private static final Tag<Short> REQUIRED_PLAYER_RANGE = Tag.Short("RequiredPlayerRange").defaultValue((short) 16);
    private static final Tag<Short> SPAWN_COUNT = Tag.Short("SpawnCount").defaultValue((short) 4);
    private static final Tag<EntityData> SPAWN_DATA = Tag.Structure("SpawnData", new EntityDataSerializer());
    private static final Tag<List<EntitySpawnList>> SPAWN_POTENTIALS = Tag.Structure("SpawnPotentials", new EntitySpawnListSerializer()).list();
    private static final Tag<Short> SPAWN_RANGE = Tag.Short("SpawnRange").defaultValue((short) 4);

    private record EntityData(String id) {
    }

    private static final class EntityDataSerializer implements TagSerializer<EntityData> {
        private final Tag<BinaryTag> PARENT = Tag.NBT("entity");

        @Override
        public @Nullable EntityData read(TagReadable reader) {
            BinaryTag nbt = reader.getTag(PARENT);
            if (nbt instanceof CompoundBinaryTag nbtCompound) {
                return new EntityData(nbtCompound.getString("id"));
            }
            return null;
        }

        // SpawnData: { entity: {id: "minecraft:pig"}}

        @Override
        public void write(TagWritable writer, EntityData value) {
            writer.setTag(PARENT, CompoundBinaryTag.from(Map.of("id", StringBinaryTag.stringBinaryTag(value.id))));
        }
    }

    private record CustomSpawnRules(int block_light_limit, int sky_light_limit) {
    }

    private static final class SpawnRulesSerializer implements TagSerializer<CustomSpawnRules> {
        private final Tag<Integer> BLOCK_LIGHT_LIMIT = Tag.Integer("block_light_limit");
        private final Tag<Integer> SKY_LIGHT_LIMIT = Tag.Integer("sky_light_limit");

        @Override
        public CustomSpawnRules read(TagReadable reader) {
            int blockLight = reader.getTag(BLOCK_LIGHT_LIMIT);
            int skyLight = reader.getTag(SKY_LIGHT_LIMIT);
            return new CustomSpawnRules(blockLight, skyLight);
        }

        @Override
        public void write(TagWritable writer, CustomSpawnRules value) {
            writer.setTag(BLOCK_LIGHT_LIMIT, value.block_light_limit);
            writer.setTag(SKY_LIGHT_LIMIT, value.sky_light_limit);
        }
    }

    private record EntitySpawnData(EntityData entity, CustomSpawnRules custom_spawn_rules) {
    }

    private static class SpawnDataSerializer implements TagSerializer<EntitySpawnData> {
        private final Tag<EntityData> ENTITY_DATA = Tag.Structure("entity", new EntityDataSerializer());
        private final Tag<CustomSpawnRules> CUSTOM_SPAWN_LIGHT = Tag.Structure("custom_spawn_rules", new SpawnRulesSerializer());

        @Override
        public EntitySpawnData read(TagReadable reader) {
            EntityData entityData = reader.getTag(ENTITY_DATA);
            CustomSpawnRules customSpawnRules = reader.getTag(CUSTOM_SPAWN_LIGHT);

            return new EntitySpawnData(entityData, customSpawnRules);
        }

        @Override
        public void write(TagWritable writer, EntitySpawnData value) {
            writer.setTag(ENTITY_DATA, value.entity);
            writer.setTag(CUSTOM_SPAWN_LIGHT, value.custom_spawn_rules);
        }
    }

    private record EntitySpawnList(int weight, EntitySpawnData data) {
    }

    private static class EntitySpawnListSerializer implements TagSerializer<EntitySpawnList> {
        private final Tag<Integer> WEIGHT = Tag.Integer("weight");
        private final Tag<EntitySpawnData> DATA = Tag.Structure("data", new SpawnDataSerializer());

        @Override
        public EntitySpawnList read(TagReadable reader) {
            int weight = reader.getTag(WEIGHT);
            EntitySpawnData entitySpawnData = reader.getTag(DATA);

            return new EntitySpawnList(weight, entitySpawnData);
        }

        @Override
        public void write(TagWritable writer, EntitySpawnList value) {
            writer.setTag(WEIGHT, value.weight);
            writer.setTag(DATA, value.data);
        }
    }
}
