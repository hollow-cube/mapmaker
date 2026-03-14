package net.hollowcube.worldconverter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.biome.BiomeInfo;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWorldAccess;
import net.hollowcube.polar.PolarWriter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.anvil.RegionFileWrapper;
import net.minestom.server.item.Material;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    static {
        MinecraftServer.init(); // We use a minestom instance to convert worlds so need registries to be loaded.
    }

    private static final Map<Long, ListBinaryTag> entitiesByChunk = new HashMap<>();
    private static final Map<String, String> biomeRenames = new HashMap<>();
    private static final List<BiomeInfo> customBiomes = new ArrayList<>();

    static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java -jar world-converter.jar <input_world_path> <output_world_path>");
            return;
        }

        var inputWorldPath = Path.of(args[0]);
        var outputWorldPath = Path.of(args[1]);

        var instance = new InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD);
        findCustomBiomes(inputWorldPath);
        if (!loadInputChunks(inputWorldPath, instance)) return;

        var loader = new PolarLoader(new PolarWorld()).setParallel(true);
        loader.setWorldAccess(new PolarWorldAccess() {
            @Override
            public void saveWorldData(Instance instance, NetworkBuffer buffer) {
                buffer.write(NetworkBuffer.BYTE, (byte) 5); // Latest as of writing
                buffer.write(NetworkBuffer.VAR_INT, MinecraftServer.DATA_VERSION);

                instance.setTag(DFU.Tag(BiomeInfo.CODEC, "biomes").list(), customBiomes);
                var worldData = instance.tagHandler().asCompound();
                buffer.write(NetworkBuffer.NBT, worldData);
            }

            @Override
            public void saveChunkData(Chunk chunk, NetworkBuffer buffer) {
                var chunkIndex = CoordConversion.chunkIndex(chunk.getChunkX(), chunk.getChunkZ());
                buffer.write(NetworkBuffer.VAR_INT, 5); // Latest as of writing

                CompoundBinaryTag.Builder tag = CompoundBinaryTag.builder();
                tag.put("entities", entitiesByChunk.getOrDefault(chunkIndex, ListBinaryTag.empty()));

                buffer.write(NetworkBuffer.NBT, tag.build());
            }

            @Override
            @SuppressWarnings("UnstableApiUsage")
            public void saveHeightmaps(Chunk chunk, int[][] heightmaps) {

                // heightmaps[Heightmaps.WORLD_SURFACE] = chunk.saveHeightmap(Heightmaps.WORLD_SURFACE);
                // heightmaps[Heightmaps.MOTION_BLOCKING] = chunk.saveHeightmap(Heightmaps.MOTION_BLOCKING);
                // heightmaps[WORLD_BOTTOM] = chunk.saveHeightmap(Heightmaps.WORLD_BOTTOM);

                // todo probably should implement
                PolarWorldAccess.super.saveHeightmaps(chunk, heightmaps);
            }

            @Override
            public String getBiomeName(int id) {
                var name = MinecraftServer.getBiomeRegistry().getKey(id).name();
                if (biomeRenames.containsKey(name)) {
                    return biomeRenames.get(name);
                }

                return PolarWorldAccess.super.getBiomeName(id);
            }
        });
        instance.setChunkLoader(loader);
        instance.saveInstance().join();

        Files.write(outputWorldPath, PolarWriter.write(loader.world()));
        logger.info("Done!");
        System.exit(0);
    }

    private static boolean loadInputChunks(Path path, InstanceContainer instance) throws Exception {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            logger.error("Input world path does not exist or is not a directory: {}", path);
            return false;
        }

        Map<Long, Chunk> chunks = stealChunkMap(instance);

        try (var fileStream = Files.walk(path.resolve("region"))) {
            int i = 0;
            var files = fileStream.toList();
            for (var regionFilePath : files) {
                i++;
                if (!Files.isRegularFile(regionFilePath) || !regionFilePath.getFileName().toString().endsWith(".mca"))
                    continue; // Skip non-region files
                var xz = parseRegionName(regionFilePath.getFileName().toString());

                var chunkRegion = new RegionFileWrapper(regionFilePath);
                var entityRegionPath = path.resolve("entities/r." + xz.x + "." + xz.z + ".mca");
                RegionFileWrapper entityRegion = Files.exists(entityRegionPath)
                        ? new RegionFileWrapper(entityRegionPath) : null;

                logger.info("Loading region at {}.{} ({}/{})", xz.x, xz.z, i, files.size());
                for (int chunkX = 0; chunkX < 32; chunkX++) {
                    for (int chunkZ = 0; chunkZ < 32; chunkZ++) {
                        var chunkData = chunkRegion.readChunkData(chunkX, chunkZ);
                        if (chunkData == null) continue;

                        if (MinecraftServer.DATA_VERSION != chunkData.getInt("DataVersion")) {
                            logger.warn("Chunk at {}.{} in region {} is using an unsupported data version: {}",
                                    chunkX, chunkZ, xz, chunkData.getInt("DataVersion"));
                            continue; // Skip unsupported chunks
                        }

                        var chunk = AnvilLoader.loadChunk(instance, xz.x * 32 + chunkX, xz.z * 32 + chunkZ, chunkData);
                        // TODO: Evaluating loadChunk, there was nowhere it could return null,
                        //  as createChunk can never return null, so should this be a try catch instead?
//                        if (chunk == null) {
//                            logger.warn("Failed to load chunk at {}.{} in region {}", chunkX, chunkZ, xz);
//                            continue; // Skip failed chunks
//                        }

                        chunks.put(CoordConversion.chunkIndex(xz.x * 32 + chunkX, xz.z * 32 + chunkZ), chunk);

                        if (entityRegion == null) continue;

                        var entityChunk = entityRegion.readChunkData(chunkX, chunkZ);
                        if (entityChunk != null) {
                            entitiesByChunk.put(CoordConversion.chunkIndex(xz.x * 32 + chunkX, xz.z * 32 + chunkZ), entityChunk.getList("Entities"));
                        }
                    }
                }
            }
        }
        return true;
    }

    private static void findCustomBiomes(Path path) throws Exception {
        // TODO: Files.walk should be used in try-with-resources
        var allBiomeOverrides = Files.walk(path.resolve("datapacks"))
                .filter(Files::isDirectory)
                .map(p -> p.resolve("data/minecraft/worldgen/biome"))
                .filter(Files::isDirectory)
                .flatMap(p -> {
                    try {
                        return Files.walk(p).filter(java.nio.file.Files::isRegularFile);
                    } catch (Exception e) {
                        logger.error("Failed to read biome files in {}", p, e);
                        return null;
                    }
                })
                .toList();

        int i = 0;
        for (var biomeFile : allBiomeOverrides) {
            var biome = Biome.REGISTRY_CODEC.decode(Transcoder.JSON, new Gson()
                            .fromJson(Files.readString(biomeFile), JsonObject.class))
                    .orElseThrow();

            var newName = "custom:import" + (i++);
            // TODO: Update for new biome attributes system
            var biomeInfo = new BiomeInfo(
                    newName, Material.STONE,
                    biome.effects().skyColor(),
                    biome.effects().fogColor(),
                    biome.effects().waterColor(),
                    biome.effects().waterFogColor(),
                    biome.effects().grassColor(),
                    biome.effects().foliageColor()
            );
            var key = biomeFile.getFileName().toString().replace(".json", "");
            biomeRenames.put("minecraft:" + key, newName);
            customBiomes.add(biomeInfo);

            System.out.println("minecraft:" + key + " -> " + newName);
        }

    }

    record XZ(int x, int z) {
    }

    private static XZ parseRegionName(String regionName) {
        if (!regionName.startsWith("r.")) {
            throw new IllegalArgumentException("Invalid region name: " + regionName);
        }
        String[] parts = regionName.substring(2).split("\\.");
        if (parts.length != 3) { // x, z, mca
            throw new IllegalArgumentException("Invalid region name format: " + regionName);
        }
        int x = Integer.parseInt(parts[0]);
        int z = Integer.parseInt(parts[1]);
        return new XZ(x, z);
    }

    private static Map<Long, Chunk> stealChunkMap(InstanceContainer instance) throws Exception {
        var field = InstanceContainer.class.getDeclaredField("chunks");
        field.setAccessible(true);
        return (Map<Long, Chunk>) field.get(instance);
    }
}
