package net.hollowcube.mapmaker.map.cylone;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.polar.*;
import net.kyori.adventure.nbt.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.palette.Palettes;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.kyori.adventure.nbt.StringBinaryTag.stringBinaryTag;

public class SlimeToPolar {

    static {
        MinecraftServer.init();
    }

    private static final Map<String, String> PATTERN_UPDATE = new HashMap<>();

    static {
        PATTERN_UPDATE.put("b", "minecraft:base");
        PATTERN_UPDATE.put("bl", "minecraft:square_bottom_left");
        PATTERN_UPDATE.put("br", "minecraft:square_bottom_right");
        PATTERN_UPDATE.put("tl", "minecraft:square_top_left");
        PATTERN_UPDATE.put("tr", "minecraft:square_top_right");
        PATTERN_UPDATE.put("bs", "minecraft:stripe_bottom");
        PATTERN_UPDATE.put("ts", "minecraft:stripe_top");
        PATTERN_UPDATE.put("ls", "minecraft:stripe_left");
        PATTERN_UPDATE.put("rs", "minecraft:stripe_right");
        PATTERN_UPDATE.put("cs", "minecraft:stripe_center");
        PATTERN_UPDATE.put("ms", "minecraft:stripe_middle");
        PATTERN_UPDATE.put("drs", "minecraft:stripe_downright");
        PATTERN_UPDATE.put("dls", "minecraft:stripe_downleft");
        PATTERN_UPDATE.put("ss", "minecraft:small_stripes");
        PATTERN_UPDATE.put("cr", "minecraft:cross");
        PATTERN_UPDATE.put("sc", "minecraft:straight_cross");
        PATTERN_UPDATE.put("bt", "minecraft:triangle_bottom");
        PATTERN_UPDATE.put("tt", "minecraft:triangle_top");
        PATTERN_UPDATE.put("bts", "minecraft:triangles_bottom");
        PATTERN_UPDATE.put("tts", "minecraft:triangles_top");
        PATTERN_UPDATE.put("ld", "minecraft:diagonal_left");
        PATTERN_UPDATE.put("rd", "minecraft:diagonal_up_right");
        PATTERN_UPDATE.put("lud", "minecraft:diagonal_up_left");
        PATTERN_UPDATE.put("rud", "minecraft:diagonal_right");
        PATTERN_UPDATE.put("mc", "minecraft:circle");
        PATTERN_UPDATE.put("mr", "minecraft:rhombus");
        PATTERN_UPDATE.put("vh", "minecraft:half_vertical");
        PATTERN_UPDATE.put("hh", "minecraft:half_horizontal");
        PATTERN_UPDATE.put("vhr", "minecraft:half_vertical_right");
        PATTERN_UPDATE.put("hhb", "minecraft:half_horizontal_bottom");
        PATTERN_UPDATE.put("bo", "minecraft:border");
        PATTERN_UPDATE.put("cbo", "minecraft:curly_border");
        PATTERN_UPDATE.put("gra", "minecraft:gradient");
        PATTERN_UPDATE.put("gru", "minecraft:gradient_up");
        PATTERN_UPDATE.put("bri", "minecraft:bricks");
        PATTERN_UPDATE.put("glb", "minecraft:globe");
        PATTERN_UPDATE.put("cre", "minecraft:creeper");
        PATTERN_UPDATE.put("sku", "minecraft:skull");
        PATTERN_UPDATE.put("flo", "minecraft:flower");
        PATTERN_UPDATE.put("moj", "minecraft:mojang");
        PATTERN_UPDATE.put("pig", "minecraft:piglin");
    }


    private static final String[] BANNER_COLOURS = new String[]{
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black",
    };

    public static String getBannerColour(final int id) {
        return id >= 0 && id < BANNER_COLOURS.length ? BANNER_COLOURS[id] : BANNER_COLOURS[0];
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
                var x = upgradedTileEntity.getInt("x");
                var y = upgradedTileEntity.getInt("y");
                var z = upgradedTileEntity.getInt("z");
                var id = upgradedTileEntity.getString("id");
                if (id.isEmpty()) throw new RuntimeException("uh oh bad");

                if (x >> 4 != pos.x() || z >> 4 != pos.z()) continue;

                blockEntities.add(new PolarChunk.BlockEntity(x & 0xF, y, z & 0xF, id, upgradedTileEntity));
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

            for (var entity : entities) {
                var entityTag = ((CompoundBinaryTag) entity);
                if (entityTag.getString("id").equals("minecraft:painting")) {
                    var pos2 = entityTag.getList("Pos");
                    System.out.println("/tp " + pos2.getInt(0) + " " + pos2.getInt(1) + " " + pos2.getInt(2) + ": " + entityTag.getString("variant"));
                }
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
        var loader = new PolarLoader(world)
                .setWorldAccess(new PolarWorldAccess() {
                    private byte[] worldData;
                    private final Map<IntIntPair, byte[]> chunkData = new HashMap<>();
                    private final Map<IntIntPair, int[][]> chunkHeightMaps = new HashMap<>();

                    @Override
                    public void loadWorldData(@NotNull Instance instance, @Nullable NetworkBuffer userData) {
                        worldData = userData.read(NetworkBuffer.RAW_BYTES);
                    }

                    @Override
                    public void saveWorldData(@NotNull Instance instance, @NotNull NetworkBuffer userData) {
                        userData.write(NetworkBuffer.RAW_BYTES, worldData);
                    }

                    @Override
                    public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer userData) {
                        chunkData.put(IntIntPair.of(chunk.getChunkX(), chunk.getChunkZ()), userData.read(NetworkBuffer.RAW_BYTES));
                    }

                    @Override
                    public void saveChunkData(@NotNull Chunk chunk, @NotNull NetworkBuffer userData) {
                        userData.write(NetworkBuffer.RAW_BYTES, chunkData.get(IntIntPair.of(chunk.getChunkX(), chunk.getChunkZ())));
                    }

                    @Override
                    public void loadHeightmaps(@NotNull Chunk chunk, int[][] heightmaps) {
                        chunkHeightMaps.put(IntIntPair.of(chunk.getChunkX(), chunk.getChunkZ()), heightmaps);
                    }

                    @Override
                    public void saveHeightmaps(@NotNull Chunk chunk, int[][] heightmaps) {
                        System.arraycopy(heightmaps, 0, chunkHeightMaps.get(IntIntPair.of(chunk.getChunkX(), chunk.getChunkZ())), 0, heightmaps.length);
                    }
                });
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

        for (var tileEntity : slimeWorld.blockEntities()) {
            var upgradedTileEntity = MCDataConverter.convertTag(MCTypeRegistry.TILE_ENTITY, tileEntity, slimeWorld.dataVersion(), MapWorld.DATA_VERSION);
            var x = upgradedTileEntity.getInt("x");
            var y = upgradedTileEntity.getInt("y");
            var z = upgradedTileEntity.getInt("z");
            var id = upgradedTileEntity.getString("id");
            if (id.isEmpty()) throw new RuntimeException("uh oh bad");

            if (upgradedTileEntity.getString("id").equals("minecraft:banner")) {
                var newPatterns = ListBinaryTag.builder();
                for (var patternTag : upgradedTileEntity.getList("patterns")) {
                    var pattern = ((CompoundBinaryTag) patternTag);
                    var newPattern = pattern.put("pattern", stringBinaryTag(Objects.requireNonNull(PATTERN_UPDATE.get(pattern.getString("Pattern")))))
                            .remove("Pattern")
                            .put("color", stringBinaryTag(Objects.requireNonNull(getBannerColour(pattern.getInt("Color")))))
                            .remove("Color");
                    newPatterns.add(newPattern);
                }
                upgradedTileEntity = upgradedTileEntity.put("patterns", newPatterns.build());
            }

            var block = instance.getBlock(x, y, z)
                    .withHandler(BlockHandler.Dummy.get(id))
                    .withNbt(upgradedTileEntity);
            instance.setBlock(x, y, z, block);
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
