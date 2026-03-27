package net.hollowcube.worldconverter;


import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.adventure.MinestomAdventure;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.palette.Palettes;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AnvilLoader {
    private final static Logger LOGGER = LoggerFactory.getLogger(net.minestom.server.instance.anvil.AnvilLoader.class);
    private static final DynamicRegistry<Biome> BIOME_REGISTRY = MinecraftServer.getBiomeRegistry();
    private final static int PLAINS_ID = BIOME_REGISTRY.getId(RegistryKey.unsafeOf("minecraft:plains"));

    public static @Nullable Chunk loadChunk(@NotNull Instance instance, int chunkX, int chunkZ, @NotNull CompoundBinaryTag chunkData) throws Exception {
        return loadMCA(instance, chunkX, chunkZ, chunkData);
    }

    private static @Nullable Chunk loadMCA(Instance instance, int chunkX, int chunkZ, @NotNull CompoundBinaryTag chunkData) throws IOException {
        // Load the chunk data (assuming it is fully generated)
        final Chunk chunk = instance.getChunkSupplier().createChunk(instance, chunkX, chunkZ);
        synchronized (chunk) { // todo: boo, synchronized
            final String status = chunkData.getString("status");

            // TODO: Should we handle other statuses?
            if (status.isEmpty() || "minecraft:full".equals(status)) {
                // TODO: Parallelize block, block entities and biome loading
                // Blocks + Biomes
                loadSections(chunk, chunkData);
                // Block entities
                loadBlockEntities(chunk, chunkData);

                chunk.loadHeightmapsFromNBT(chunkData.getCompound("Heightmaps"));
            } else {
                LOGGER.warn("Skipping partially generated chunk at {}, {} with status {}", chunkX, chunkZ, status);
            }
        }

        return chunk;
    }

    private static void loadSections(@NotNull Chunk chunk, @NotNull CompoundBinaryTag chunkData) {
        for (BinaryTag sectionTag : chunkData.getList("sections", BinaryTagTypes.COMPOUND)) {
            final CompoundBinaryTag sectionData = (CompoundBinaryTag) sectionTag;

            final int sectionY = sectionData.getInt("Y", Integer.MIN_VALUE);
            Check.stateCondition(sectionY == Integer.MIN_VALUE, "Missing section Y value");
            final int yOffset = Chunk.CHUNK_SECTION_SIZE * sectionY;

            if (sectionY < chunk.getMinSection() || sectionY >= chunk.getMaxSection()) {
                // Vanilla stores a section below and above the world for lighting, throw it out.
                continue;
            }

            final Section section = chunk.getSection(sectionY);

            // Lighting
            if (sectionData.get("SkyLight") instanceof ByteArrayBinaryTag skyLightTag && skyLightTag.size() == 2048) {
                section.setSkyLight(skyLightTag.value());
            }
            if (sectionData.get("BlockLight") instanceof ByteArrayBinaryTag blockLightTag && blockLightTag.size() == 2048) {
                section.setBlockLight(blockLightTag.value());
            }

            {   // Biomes
                final CompoundBinaryTag biomesTag = sectionData.getCompound("biomes");
                final ListBinaryTag biomePaletteTag = biomesTag.getList("palette", BinaryTagTypes.STRING);
                int[] convertedBiomePalette = loadBiomePalette(biomePaletteTag);

                if (convertedBiomePalette.length == 1) {
                    // One solid block, no need to check the data
                    section.biomePalette().fill(convertedBiomePalette[0]);
                } else if (convertedBiomePalette.length > 1) {
                    final long[] packedIndices = biomesTag.getLongArray("data");
                    Check.stateCondition(packedIndices.length == 0, "Missing packed biomes data");
                    int[] biomeIndices = new int[64];

                    int bitsPerEntry = MathUtils.bitsToRepresent(convertedBiomePalette.length - 1);
                    Palettes.unpack(biomeIndices, packedIndices, bitsPerEntry);

                    for (int y = 0; y < Chunk.CHUNK_SECTION_SIZE / 4; y++) {
                        for (int z = 0; z < Chunk.CHUNK_SECTION_SIZE / 4; z++) {
                            for (int x = 0; x < Chunk.CHUNK_SECTION_SIZE / 4; x++) {
                                final int index = x + z * 4 + y * 16;
                                final int biomeId = convertedBiomePalette[biomeIndices[index]];
                                section.biomePalette().set(x, y, z, biomeId);
                            }
                        }
                    }
                }
            }

            {   // Blocks
                final CompoundBinaryTag blockStatesTag = sectionData.getCompound("block_states");
                final ListBinaryTag blockPaletteTag = blockStatesTag.getList("palette", BinaryTagTypes.COMPOUND);
                Block[] convertedPalette = loadBlockPalette(blockPaletteTag);
                if (blockPaletteTag.size() == 1) {
                    // One solid block, no need to check the data
                    section.blockPalette().fill(convertedPalette[0].stateId());
                } else if (blockPaletteTag.size() > 1) {
                    final long[] packedStates = blockStatesTag.getLongArray("data");
                    Check.stateCondition(packedStates.length == 0, "Missing packed states data");
                    int[] blockStateIndices = new int[Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE];
                    Palettes.unpack(blockStateIndices, packedStates, packedStates.length * 64 / blockStateIndices.length);

                    for (int y = 0; y < Chunk.CHUNK_SECTION_SIZE; y++) {
                        for (int z = 0; z < Chunk.CHUNK_SECTION_SIZE; z++) {
                            for (int x = 0; x < Chunk.CHUNK_SECTION_SIZE; x++) {
                                try {
                                    final int blockIndex = y * Chunk.CHUNK_SECTION_SIZE * Chunk.CHUNK_SECTION_SIZE + z * Chunk.CHUNK_SECTION_SIZE + x;
                                    final int paletteIndex = blockStateIndices[blockIndex];
                                    final Block block = convertedPalette[paletteIndex];

                                    chunk.setBlock(x, y + yOffset, z, block);
                                } catch (Exception e) {
                                    MinecraftServer.getExceptionManager().handleException(e);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static Block[] loadBlockPalette(@NotNull ListBinaryTag paletteTag) {
        Block[] convertedPalette = new Block[paletteTag.size()];
        for (int i = 0; i < convertedPalette.length; i++) {
            CompoundBinaryTag paletteEntry = paletteTag.getCompound(i);
            String blockName = paletteEntry.getString("Name");
            if (blockName.equals("minecraft:air")) {
                convertedPalette[i] = Block.AIR;
            } else {
                Block block = Objects.requireNonNull(Block.fromKey(blockName), "Unknown block " + blockName);
                // Properties
                final Map<String, String> properties = new HashMap<>();
                CompoundBinaryTag propertiesNBT = paletteEntry.getCompound("Properties");
                for (var property : propertiesNBT) {
                    if (property.getValue() instanceof StringBinaryTag propertyValue) {
                        properties.put(property.getKey(), propertyValue.value());
                    } else {
                        try {
                            LOGGER.warn("Fail to parse block state properties {}, expected a string for {}, but contents were {}",
                                    propertiesNBT, property.getKey(), MinestomAdventure.tagStringIO().asString(property.getValue()));
                        } catch (Exception e) {
                            LOGGER.warn("Fail to parse block state properties {}", propertiesNBT, e);
                        }
                    }
                }
                if (!properties.isEmpty()) block = block.withProperties(properties);

                // Handler
                final BlockHandler handler = MinecraftServer.getBlockManager().getHandler(block.name());
                if (handler != null) block = block.withHandler(handler);

                convertedPalette[i] = block;
            }
        }
        return convertedPalette;
    }

    private static int[] loadBiomePalette(@NotNull ListBinaryTag paletteTag) {
        int[] convertedPalette = new int[paletteTag.size()];
        for (int i = 0; i < convertedPalette.length; i++) {
            final String name = paletteTag.getString(i);
            int biomeId = BIOME_REGISTRY.getId(RegistryKey.unsafeOf(name));
            if (biomeId == -1) biomeId = PLAINS_ID;
            convertedPalette[i] = biomeId;
        }
        return convertedPalette;
    }

    private static void loadBlockEntities(@NotNull Chunk loadedChunk, @NotNull CompoundBinaryTag chunkData) {
        for (BinaryTag blockEntityTag : chunkData.getList("block_entities", BinaryTagTypes.COMPOUND)) {
            final CompoundBinaryTag blockEntity = (CompoundBinaryTag) blockEntityTag;

            final int x = blockEntity.getInt("x");
            final int y = blockEntity.getInt("y");
            final int z = blockEntity.getInt("z");
            Block block = loadedChunk.getBlock(x, y, z);

            // Load the block handler if the id is present
            if (blockEntity.get("id") instanceof StringBinaryTag blockEntityId) {
                final BlockHandler handler = MinecraftServer.getBlockManager().getHandlerOrDummy(blockEntityId.value());
                block = block.withHandler(handler);
            }

            // Remove anvil tags
            CompoundBinaryTag trimmedTag = CompoundBinaryTag.builder().put(blockEntity)
                    .remove("id").remove("keepPacked")
                    .remove("x").remove("y").remove("z")
                    .build();

            // Place block
            final var finalBlock = trimmedTag.size() > 0 ? block.withNbt(trimmedTag) : block;
            loadedChunk.setBlock(x, y, z, finalBlock);
        }
    }

}

