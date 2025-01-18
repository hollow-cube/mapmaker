package net.hollowcube.mapmaker.map.cylone;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.polar.*;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palettes;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public class SlimeToPolar {

    static {
        MinecraftServer.init();
    }

    public static byte[] convertSlimeToPolar(byte[] slimeWorldData, JsonObject mapData) throws Exception {
        var slimeWorld = SlimeReader.read(slimeWorldData);

        var chunks = new ArrayList<PolarChunk>();
        for (var slimeChunkPair : slimeWorld.chunks().entrySet()) {
            var pos = slimeChunkPair.getKey();
            var sections = new PolarSection[24];
            for (int i = 0; i < 24; i++) {
                var section = slimeChunkPair.getValue().sections()[i];

                String[] blockPalette;
                int[] blockData;
                var paletteB = section.blockStateTag().getList("palette");
                if (paletteB.size() == 1) {
                    blockPalette = new String[]{blockPropertiesToString(paletteB.getCompound(0))};
                    blockData = null;
                } else {
                    blockPalette = paletteB.stream()
                            .map(t -> (CompoundBinaryTag) t)
                            .map(SlimeToPolar::blockPropertiesToString)
                            .toArray(String[]::new);

                    var rawBlockData = section.blockStateTag().getLongArray("data");
                    blockData = new int[4096];
                    Palettes.unpack(blockData, rawBlockData, rawBlockData.length * 64 / blockData.length);
                }
                for (int idx = 0; idx < blockPalette.length; idx++) {
                    blockPalette[idx] = (String) MCDataConverter.convert(MCTypeRegistry.FLAT_BLOCK_STATE,
                            blockPalette[idx], slimeWorld.dataVersion(), MapWorld.DATA_VERSION);
                }

                String[] biomePalette;
                int[] biomeData;
                var paletteBi = section.biomeTag().getList("palette");
                if (paletteBi.size() == 1) {
                    biomePalette = new String[]{paletteBi.getString(0)};
                    biomeData = null;
                } else {
                    throw new UnsupportedOperationException("todo");
                }
                for (int idx = 0; idx < biomePalette.length; idx++) {
                    biomePalette[idx] = (String) MCDataConverter.convert(MCTypeRegistry.BIOME,
                            biomePalette[idx], slimeWorld.dataVersion(), MapWorld.DATA_VERSION);
                }

                PolarSection.LightContent blockLightContent = PolarSection.LightContent.MISSING;
                byte[] blockLight = null;
                if (section.blockLightArray() != null && section.blockLightArray().length == 2048) {
                    blockLight = section.blockLightArray();
                }

                PolarSection.LightContent skyLightContent = PolarSection.LightContent.MISSING;
                byte[] skyLight = null;
                if (section.skyLightArray() != null && section.skyLightArray().length == 2048) {
                    skyLight = section.skyLightArray();
                }

                sections[i] = new PolarSection(blockPalette, blockData, biomePalette, biomeData,
                        blockLightContent, blockLight, skyLightContent, skyLight);
            }

            var blockEntities = new ArrayList<PolarChunk.BlockEntity>();
            for (var tileEntity : slimeWorld.blockEntities()) {
                var upgradedTileEntity = MCDataConverter.convertTag(MCTypeRegistry.TILE_ENTITY, tileEntity, slimeWorld.dataVersion(), MapWorld.DATA_VERSION);
                var x = upgradedTileEntity.getInt("x") >> 4;
                var y = upgradedTileEntity.getInt("y");
                var z = upgradedTileEntity.getInt("z") >> 4;
                var id = upgradedTileEntity.getString("id");
                if (id.isEmpty()) throw new RuntimeException("uh oh bad");

                if (x != pos.x() || z != pos.z()) continue;

                blockEntities.add(new PolarChunk.BlockEntity(x, y, z, id, upgradedTileEntity));
            }

            var entities = new ArrayList<BinaryTag>();
            for (var slimeEntity : slimeWorld.entities()) {
                var posTag = slimeEntity.getList("Pos", BinaryTagTypes.DOUBLE);
                var x = posTag.getInt(0) >> 4;
                var z = posTag.getInt(2) >> 4;

                if (x != pos.x() || z != pos.z()) continue;

                var upgradedEntity = MCDataConverter.convertTag(MCTypeRegistry.ENTITY,
                        slimeEntity, slimeWorld.dataVersion(), MapWorld.DATA_VERSION);
                entities.add(upgradedEntity);
            }

            var chunkData = NetworkBuffer.makeArray(buffer -> {
                buffer.write(NetworkBuffer.VAR_INT, 5);
                buffer.write(NetworkBuffer.NBT, CompoundBinaryTag.builder()
                        .put("entities", ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, entities))
                        .build());
            });

            chunks.add(new PolarChunk(pos.x(), pos.z(), sections, blockEntities, new int[32][], chunkData));
        }

        var worldData = NetworkBuffer.makeArray(buffer -> {
            buffer.write(NetworkBuffer.BYTE, (byte) 5);
            buffer.write(NetworkBuffer.VAR_INT, MapWorld.DATA_VERSION);
            buffer.write(NetworkBuffer.NBT, CompoundBinaryTag.empty());
        });

        var world = new PolarWorld(PolarWorld.LATEST_VERSION, MapWorld.DATA_VERSION,
                PolarWorld.CompressionType.ZSTD, (byte) -4, (byte) 19,
                worldData, chunks);

        var instance = new InstanceContainer(UUID.randomUUID(), DimensionType.OVERWORLD);
        var loader = new PolarLoader(world);
        instance.setChunkLoader(loader);

        for (var chunk : loader.world().chunks()) {
            instance.loadChunk(chunk.x(), chunk.z()).join();
        }

        if (mapData.has("checkpoints")) {
            for (var checkpointRaw : mapData.getAsJsonArray("checkpoints")) {
                var checkpoint = checkpointRaw.getAsJsonObject();
                var x = checkpoint.get("x").getAsInt();
                var y = checkpoint.get("y").getAsInt();
                var z = checkpoint.get("z").getAsInt();

                instance.setBlock(x, y, z, Block.HEAVY_WEIGHTED_PRESSURE_PLATE
                        .withHandler(new CheckpointPlateBlock()));
            }
        }

        instance.saveChunksToStorage().join();

        return PolarWriter.write(loader.world());
    }

    private static String blockPropertiesToString(@NotNull CompoundBinaryTag tag) {
        var name = tag.getString("Name");
        var properties = tag.getCompound("Properties");
        if (properties.size() > 0) {
            var builder = new StringBuilder().append(name).append('[');
            for (var entry : properties) {
                builder.append(entry.getKey()).append('=').append(((StringBinaryTag) entry.getValue()).value()).append(',');
            }
            builder.setLength(builder.length() - 1);
            return builder.append(']').toString();
        }
        return name;
    }
}
