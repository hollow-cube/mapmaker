package net.hollowcube.worldconverter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.map.ReadableMapData;
import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.entity.MapEntities;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.map.polar.ReadWriteWorldAccess;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.serializer.nbt.NbtComponentSerializer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.instance.anvil.RegionFileWrapper;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.registry.RegistryTranscoder;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.minestom.server.coordinate.CoordConversion.chunkIndex;
import static net.minestom.server.coordinate.CoordConversion.chunkToRegion;

/// Writes a Hollow Cube polar world as a vanilla world directory (26.1+ layout, overworld only).
///
/// The world is loaded the same way a map server loads it, so polar block data, world data and
/// chunk data all go through the data fixers, and entities are spawned as real map entities.
/// Entities are then written back out with the same code the editor saves with, since Minestom's
/// [AnvilLoader] has no entity support. No `level.dat` is written.
final class PolarToAnvil {
    private static final Logger logger = LoggerFactory.getLogger(PolarToAnvil.class);

    private static final Key DIMENSION = Key.key("minecraft:overworld");
    private static final int DATA_PACK_FORMAT = 121; // 26.3
    private static final Tag<String> VANILLA_STATUS = Tag.String("Status");

    static void convert(Path input, Path output) throws IOException {
        DataFixer.buildModel();
        MapEntities.init(EventNode.type("world-converter", EventFilter.INSTANCE)); // Registers the MapEntity constructors

        var biomes = new BiomeContainer();
        var instance = new MapInstance("converter", MapInstance.LightingMode.LOADED);
        try (var channel = Files.newByteChannel(input)) {
            instance.loadStream(new ReadableMapData(channel, Files.size(input)), new ReadWorldAccess(biomes::init, biomes));
        }

        // Passengers are mounted on the tick after spawn, and nothing ticks here.
        for (var entity : instance.getEntities())
            entity.scheduler().processTick();

        var chunks = List.copyOf(instance.getChunks());
        logger.info("Loaded {} chunks and {} entities", chunks.size(), instance.getEntities().size());

        writeCustomBiomes(output, biomes, chunks);

        var anvil = new AnvilLoader(output, DIMENSION);
        for (var chunk : chunks) {
            // AnvilLoader writes a lowercase `status`, which vanilla does not read, so it discards
            // every chunk as "missing level data". Chunk tags are copied into the chunk root.
            chunk.setTag(VANILLA_STATUS, "minecraft:full");
            anvil.saveChunk(chunk);
            anvil.unloadChunk(chunk); // Closes the region file once its last chunk is written
        }

        writeEntities(output.resolve("dimensions/minecraft/overworld/entities"), chunks);
        instance.unload();
    }

    /// Map biomes live in a per-map container with ids past the end of the global registry, which
    /// [AnvilLoader] cannot name. They are registered globally, remapped in every chunk, and
    /// written as a datapack so vanilla knows them.
    private static void writeCustomBiomes(Path output, BiomeContainer biomes, List<Chunk> chunks) throws IOException {
        var registry = MinecraftServer.getBiomeRegistry();
        var remap = new Int2IntOpenHashMap();
        var json = new RegistryTranscoder<>(Transcoder.JSON, MinecraftServer.process());
        var gson = new GsonBuilder().setPrettyPrinting().create();
        var packRoot = output.resolve("datapacks/hollowcube");

        for (var info : biomes.values()) {
            var key = info.key();
            var biome = info.build();
            if (key == null || biome == null) continue;

            int localId = biomes.getId(RegistryKey.unsafeOf(key));
            if (localId == -1) continue; // Created but never loaded, so no chunk can reference it
            remap.put(localId, registry.getId(registry.register(key, biome)));

            var biomeJson = (JsonObject) Biome.REGISTRY_CODEC.encode(json, biome).orElseThrow();
            // Vanilla requires generation settings that Minestom does not model
            biomeJson.add("carvers", new JsonArray());
            biomeJson.add("features", new JsonArray());
            biomeJson.add("spawns_by_category", new JsonObject());
            biomeJson.add("spawn_costs", new JsonObject());

            var path = packRoot.resolve("data/%s/worldgen/biome/%s.json".formatted(key.namespace(), key.value()));
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(biomeJson));
        }
        if (remap.isEmpty()) return;

        var pack = new JsonObject();
        pack.addProperty("description", "Hollow Cube map biomes");
        pack.addProperty("min_format", DATA_PACK_FORMAT);
        pack.addProperty("max_format", DATA_PACK_FORMAT);
        Files.writeString(packRoot.resolve("pack.mcmeta"), gson.toJson(Map.<String, JsonElement>of("pack", pack)));

        for (var chunk : chunks) {
            for (var section : chunk.getSections())
                section.biomePalette().replaceAll((_, _, _, id) -> remap.getOrDefault(id, id));
        }
        logger.info("Wrote {} custom biomes to {}", remap.size(), packRoot);
    }

    private static void writeEntities(Path entitiesDir, List<Chunk> chunks) throws IOException {
        var regions = new Long2ObjectOpenHashMap<List<Chunk>>();
        for (var chunk : chunks) {
            long region = chunkIndex(chunkToRegion(chunk.getChunkX()), chunkToRegion(chunk.getChunkZ()));
            regions.computeIfAbsent(region, _ -> new ArrayList<>()).add(chunk);
        }

        int written = 0;
        for (var regionChunks : regions.values()) {
            RegionFileWrapper region = null;
            try {
                for (var chunk : regionChunks) {
                    var entities = ReadWriteWorldAccess.saveEntities(chunk);
                    if (entities.size() == 0) continue;

                    if (region == null) {
                        var name = "r.%d.%d.mca".formatted(chunkToRegion(chunk.getChunkX()), chunkToRegion(chunk.getChunkZ()));
                        Files.createDirectories(entitiesDir);
                        region = new RegionFileWrapper(entitiesDir.resolve(name));
                    }

                    var vanillaEntities = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
                    for (var entity : entities) vanillaEntities.add(toVanilla((CompoundBinaryTag) entity));

                    region.writeChunkData(chunk.getChunkX(), chunk.getChunkZ(), CompoundBinaryTag.builder()
                            .putInt("DataVersion", MinecraftServer.DATA_VERSION)
                            .putIntArray("Position", new int[]{chunk.getChunkX(), chunk.getChunkZ()})
                            .put("Entities", vanillaEntities.build())
                            .build());
                    written += entities.size();
                }
            } finally {
                if (region != null) region.close();
            }
        }
        logger.info("Wrote {} root entities", written);
    }

    /// MapEntity writes a lowercase `uuid` and a JSON string `CustomName`, where vanilla wants
    /// `UUID` and a component tag. Left alone, vanilla would assign a fresh uuid and show the raw
    /// JSON as the name.
    private static CompoundBinaryTag toVanilla(CompoundBinaryTag tag) {
        var builder = CompoundBinaryTag.builder().put(tag);

        if (tag.get("uuid") instanceof IntArrayBinaryTag uuid) {
            builder.remove("uuid");
            builder.put("UUID", uuid);
        }
        if (tag.get("CustomName") instanceof StringBinaryTag name) {
            var component = GsonComponentSerializer.gson().deserialize(name.value());
            builder.put("CustomName", NbtComponentSerializer.nbt().serialize(component));
        }

        var passengers = tag.getList("Passengers", BinaryTagTypes.COMPOUND);
        if (passengers.size() > 0) {
            var fixed = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
            for (var passenger : passengers) fixed.add(toVanilla((CompoundBinaryTag) passenger));
            builder.put("Passengers", fixed.build());
        }

        return builder.build();
    }
}
