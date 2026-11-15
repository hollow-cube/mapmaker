package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.hollowcube.terraform.entity.EntityEdit;
import net.hollowcube.terraform.util.BlockUtil;
import net.hollowcube.terraform.util.GameDataProvider;
import net.hollowcube.terraform.util.TypedData;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.instance.palette.Palettes;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registry;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

import static net.hollowcube.terraform.util.BlockUtil.packBlockPos;
import static net.minestom.server.coordinate.CoordConversion.*;
import static net.minestom.server.network.NetworkBuffer.*;

/// Accessible via [EditBuffer#serializer].
///
/// Stable wire format for EditBuffers. It is versioned and guaranteed to be able to read any historical version
/// in the future.
///
/// Palette names (eg `minecraft:stone_stairs[facing=north]`) for biomes and blocks are accumulated in a global
/// table during read/write the first time they appear. Ie, a palette value may be:
/// * 0 -> unset
/// * n-1 == current table length -> new value, followed by string text for that value
/// * n-1 < current table length -> existing table entry
/// The goal is to allow future streaming reads/writes.
///
/// Values are upgraded via [GameDataProvider] during read. Unknown values resolve to air/plains respectively.
@SuppressWarnings("UnstableApiUsage")
final class EditBufferSerializer implements NetworkBuffer.Type<EditBuffer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditBufferSerializer.class);

    private static final int VERSION_LATEST = 1;

    private static final byte BLOCKS_NONE = 0;
    private static final byte BLOCKS_UNIFORM = 1;
    private static final byte BLOCKS_SPARSE = 2;
    private static final byte BLOCKS_PALETTE = 3;

    private static final byte BIOMES_NONE = 0;
    private static final byte BIOMES_PALETTE = 1;

    private static final int BLOCK_DIM = 16;
    private static final int BIOME_DIM = 4;

    private static final Key PLAINS = Key.key("minecraft", "plains");

    private final GameDataProvider gameData;
    private final Registry<Biome> biomes;

    EditBufferSerializer(GameDataProvider gameData, Registry<Biome> biomes) {
        this.gameData = gameData;
        this.biomes = biomes;
    }

    //region: Writing

    @Override
    public void write(NetworkBuffer buffer, EditBuffer value) {
        buffer.write(VAR_INT, VERSION_LATEST);
        buffer.write(VAR_INT, gameData.dataVersion());

        Area.Cuboid bounds = value.blockBounds();
        buffer.write(BOOLEAN, bounds != null);
        if (bounds != null) {
            buffer.write(BLOCK_POSITION, bounds.min());
            buffer.write(BLOCK_POSITION, bounds.max());
        }

        var blockNames = new NameWriter(id -> Block.fromStateId(id).state());
        var biomeNames = new NameWriter(this::biomeName);
        buffer.write(VAR_INT, value.sectionCount());
        value.forEachSection((sx, sy, sz, section) -> {
            buffer.write(LONG, packBlockPos(sx, sy, sz));
            writeBlocks(buffer, section, blockNames);
            writeBlockEntities(buffer, section);
            writeBiomes(buffer, section, biomeNames);
        });

        writeEntities(buffer, value.entities());
    }

    private static void writeBlocks(NetworkBuffer buffer, BufferSection section, NameWriter names) {
        if (!section.hasBlocks()) {
            buffer.write(BYTE, BLOCKS_NONE);
            return;
        }

        Palette dense = section.densePalette();
        if (dense == null) {
            buffer.write(BYTE, BLOCKS_SPARSE);
            buffer.write(VAR_INT, section.count());
            section.forEachBlockState((x, y, z, stateId) -> {
                buffer.write(SHORT, (short) sectionBlockIndex(x, y, z));
                names.write(buffer, stateId, 0);
            });
            return;
        }

        int uniform = section.uniformBlockValue();
        if (uniform != BufferSection.NONE) {
            buffer.write(BYTE, BLOCKS_UNIFORM);
            names.write(buffer, uniform - 1, 0);
            return;
        }

        buffer.write(BYTE, BLOCKS_PALETTE);
        writePalette(buffer, dense, BLOCK_DIM, names);
    }

    private static void writeBlockEntities(NetworkBuffer buffer, BufferSection section) {
        buffer.write(VAR_INT, section.blockEntityCount());
        section.forEachBlockEntity((x, y, z, block) -> {
            Key id = BlockUtil.blockEntityId(block);
            CompoundBinaryTag nbt = block.nbt();
            buffer.write(SHORT, (short) sectionBlockIndex(x, y, z));
            buffer.write(STRING, id != null ? id.asString() : "");
            buffer.write(NBT_COMPOUND, nbt != null ? nbt : CompoundBinaryTag.empty());
        });
    }

    private static void writeBiomes(NetworkBuffer buffer, BufferSection section, NameWriter names) {
        Palette biomes = section.biomePalette();
        if (biomes == null || !section.hasBiomes()) {
            buffer.write(BYTE, BIOMES_NONE);
            return;
        }

        buffer.write(BYTE, BIOMES_PALETTE);
        writePalette(buffer, biomes, BIOME_DIM, names);
    }

    private static void writeEntities(NetworkBuffer buffer, List<EntityEdit> entities) {
        buffer.write(VAR_INT, entities.size());
        for (EntityEdit edit : entities) {
            buffer.write(UUID, edit.id());
            buffer.write(POS, edit.pos());
            TypedData data = edit.data();
            buffer.write(BOOLEAN, data != null);
            if (data != null) {
                buffer.write(STRING, data.type());
                buffer.write(NBT_COMPOUND, data.nbt());
            }
        }
    }

    /// The palette's values (0 for leave, else a name reference offset by one), the bits per cell, then each cell's
    /// index in the values, packed the way [Palette#load] reads them. The values may include some no cell uses.
    private static void writePalette(
        NetworkBuffer buffer,
        Palette palette,
        int dimension,
        NameWriter names
    ) {
        IntArrayList dictionary = PaletteHack.dictionary(palette);
        int paletteBits = PaletteHack.bitsPerEntry(palette);
        if (dictionary != null && paletteBits > 0) {
            // An indirect palette already holds this encoding: its dictionary, and the cells packed as indices
            writeValues(buffer, dictionary, names);
            buffer.write(VAR_INT, paletteBits);
            buffer.write(LONG_ARRAY, PaletteHack.packed(palette));
            return;
        }

        var local = new Int2IntOpenHashMap();
        local.defaultReturnValue(-1);
        var values = new IntArrayList();
        palette.getAll((_, _, _, value) -> {
            if (local.putIfAbsent(value, values.size()) == -1) values.add(value);
        });
        writeValues(buffer, values, names);

        // Palette#getAll walks the cells in index order, so the indices pack straight into the longs
        int bits = minimalBits(values.size());
        int perLong = 64 / bits;
        buffer.write(VAR_INT, bits);
        buffer.write(VAR_INT, Palettes.arrayLength(dimension, bits));
        var packer = new long[] {0, 0}; // current long, cells in it
        palette.getAll((_, _, _, value) -> {
            packer[0] |= (long) local.get(value) << (packer[1] * bits);
            if (++packer[1] == perLong) {
                buffer.write(LONG, packer[0]);
                packer[0] = packer[1] = 0;
            }
        });
        if (packer[1] > 0) buffer.write(LONG, packer[0]);
    }

    private static void writeValues(NetworkBuffer buffer, IntArrayList values, NameWriter names) {
        buffer.write(VAR_INT, values.size());
        for (int i = 0; i < values.size(); i++) {
            int value = values.getInt(i);
            if (value == 0) buffer.write(VAR_INT, 0);
            else names.write(buffer, value - 1, 1);
        }
    }

    private static int minimalBits(int size) {
        return Math.max(1, MathUtils.bitsToRepresent(Math.max(1, size - 1)));
    }

    private String biomeName(int biomeId) {
        RegistryKey<Biome> key = biomes.getKey(biomeId);
        if (key != null) return key.key().asString();
        LOGGER.warn("biome id {} is not in the registry, written as plains", biomeId);
        return PLAINS.asString();
    }

    private static final class NameWriter {
        private final IntFunction<String> name;
        private final Int2IntOpenHashMap indices = new Int2IntOpenHashMap();

        NameWriter(IntFunction<String> name) {
            this.name = name;
            indices.defaultReturnValue(-1);
        }

        /// Writes a reference to `id`, offset by `offset` so smaller values stay free for the caller.
        void write(NetworkBuffer buffer, int id, int offset) {
            int index = indices.get(id);
            if (index != -1) {
                buffer.write(VAR_INT, index + offset);
                return;
            }
            index = indices.size();
            indices.put(id, index);
            buffer.write(VAR_INT, index + offset);
            buffer.write(STRING, name.apply(id));
        }
    }

    //endregion

    //region: Reading

    @Override
    public EditBuffer read(NetworkBuffer buffer) {
        int version = buffer.read(VAR_INT);
        if (version > VERSION_LATEST) {
            throw new IllegalStateException(
                "edit buffer was written by a newer version: %d > %d".formatted(
                    version,
                    VERSION_LATEST
                )
            );
        }
        if (version != 1) throw new IllegalStateException("unknown edit buffer version " + version);
        return new Reader(buffer.read(VAR_INT)).read(buffer);
    }

    /// One read's name tables and the names it has warned about.
    private final class Reader {
        private final int dataVersion;
        private final Set<String> warned = new HashSet<>();
        // Reference index to resolved id, growing as names are first referenced
        private final IntArrayList blockStates = new IntArrayList();
        private final IntArrayList biomeIds = new IntArrayList();

        Reader(int dataVersion) {
            this.dataVersion = dataVersion;
        }

        EditBuffer read(NetworkBuffer buffer) {
            Area.@Nullable Cuboid bounds = null;
            if (buffer.read(BOOLEAN))
                bounds = Area.cuboid(
                    blockVec(buffer.read(BLOCK_POSITION)),
                    blockVec(buffer.read(BLOCK_POSITION))
                );

            int sectionCount = buffer.read(VAR_INT);
            Long2ObjectMap<BufferSection> sections = new Long2ObjectOpenHashMap<>(sectionCount);
            for (int i = 0; i < sectionCount; i++) {
                long key = buffer.read(LONG);
                BufferSection section = readBlocks(buffer);
                readBlockEntities(buffer, section);
                readBiomes(buffer, section);
                if (section.hasBlocks() || section.hasBiomes()) sections.put(key, section);
            }

            return new EditBuffer(
                sections.isEmpty() ? Long2ObjectMaps.emptyMap() : sections,
                bounds,
                readEntities(buffer)
            );
        }

        private List<EntityEdit> readEntities(NetworkBuffer buffer) {
            int count = buffer.read(VAR_INT);
            List<EntityEdit> entities = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                var id = buffer.read(UUID);
                var pos = buffer.read(POS);
                if (!buffer.read(BOOLEAN)) {
                    entities.add(EntityEdit.remove(id, pos));
                    continue;
                }
                @Nullable TypedData data = resolveEntity(
                    buffer.read(STRING),
                    buffer.read(NBT_COMPOUND)
                );
                if (data != null) entities.add(new EntityEdit(id, pos, data));
            }
            return List.copyOf(entities);
        }

        private BufferSection readBlocks(NetworkBuffer buffer) {
            byte kind = buffer.read(BYTE);
            return switch (kind) {
                case BLOCKS_NONE -> new BufferSection();
                case BLOCKS_UNIFORM ->
                    BufferSection.uniform(readBlock(buffer, buffer.read(VAR_INT)));
                case BLOCKS_SPARSE -> {
                    var section = new BufferSection();
                    int count = buffer.read(VAR_INT);
                    for (int i = 0; i < count; i++) {
                        int index = readCell(buffer);
                        section.setBlockState(
                            sectionBlockIndexGetX(index),
                            sectionBlockIndexGetY(index),
                            sectionBlockIndexGetZ(index),
                            readBlock(buffer, buffer.read(VAR_INT))
                        );
                    }
                    yield section;
                }
                case BLOCKS_PALETTE -> new BufferSection(
                    readPalette(
                        buffer,
                        BufferSection.emptyBlockPalette(),
                        BLOCK_DIM,
                        blockStates,
                        this::resolveBlock
                    )
                );
                default ->
                    throw new IllegalStateException("unknown section block encoding " + kind);
            };
        }

        private void readBlockEntities(NetworkBuffer buffer, BufferSection section) {
            int count = buffer.read(VAR_INT);
            for (int i = 0; i < count; i++) {
                int index = readCell(buffer);
                String id = buffer.read(STRING);
                CompoundBinaryTag nbt = buffer.read(NBT_COMPOUND);
                int x = sectionBlockIndexGetX(index),
                    y = sectionBlockIndexGetY(index),
                    z = sectionBlockIndexGetZ(index);
                int stateId = section.getBlockState(x, y, z);
                if (stateId == BufferSection.NONE) continue;

                Block block = Block.fromStateId(stateId);
                if (!id.isEmpty()) {
                    var data = new TypedData(id, nbt);
                    if (dataVersion < gameData.dataVersion())
                        data = gameData.upgradeBlockEntity(
                            dataVersion,
                            gameData.dataVersion(),
                            data
                        );
                    nbt = data.nbt();
                    @Nullable BlockHandler handler = MinecraftServer.getBlockManager().getHandler(
                        data.type()
                    );
                    if (handler != null) block = block.withHandler(handler);
                }
                if (nbt.size() > 0) block = block.withNbt(nbt);
                if (!BlockUtil.isPlainState(block)) section.setBlockEntity(x, y, z, block);
            }
        }

        private void readBiomes(NetworkBuffer buffer, BufferSection section) {
            byte kind = buffer.read(BYTE);
            switch (kind) {
                case BIOMES_NONE -> {}
                case BIOMES_PALETTE -> readPalette(
                    buffer,
                    BufferSection.emptyBiomePalette(),
                    BIOME_DIM,
                    biomeIds,
                    this::resolveBiome
                ).getAllPresent((x, y, z, value) -> section.setBiome(x, y, z, value - 1));
                default ->
                    throw new IllegalStateException("unknown section biome encoding " + kind);
            }
        }

        /// Reads [EditBufferSerializer#writePalette]'s values and indices into `palette`, each value's name reference
        /// looked up in `table` (or resolved with `resolve` on first reference).
        private static Palette readPalette(
            NetworkBuffer buffer,
            Palette palette,
            int dimension,
            IntArrayList table,
            ToIntFunction<String> resolve
        ) {
            int cells = dimension * dimension * dimension;
            int size = buffer.read(VAR_INT);
            if (size < 1 || size > cells)
                throw new IllegalStateException("palette of " + size + " values");
            int[] values = new int[size];
            var seen = new IntOpenHashSet();
            boolean distinct = true;
            for (int i = 0; i < size; i++) {
                int raw = buffer.read(VAR_INT);
                values[i] = raw == 0 ? 0 : readReference(buffer, raw - 1, table, resolve) + 1;
                distinct &= seen.add(values[i]);
            }
            int bits = buffer.read(VAR_INT);
            if (bits < minimalBits(size) || bits > 16)
                throw new IllegalStateException(bits + " bits per cell for " + size + " values");
            long[] packed = buffer.read(LONG_ARRAY);
            if (packed.length != Palettes.arrayLength(dimension, bits))
                throw new IllegalStateException("palette indices are " + packed.length + " longs");

            if (distinct && bits == minimalBits(size)) {
                palette.load(values, packed);
                return palette;
            }
            // Two names can resolve to the same value (both unknown, say), which a palette can't hold twice, and
            // the cells can be packed wider than a palette would load them
            int[] indices = new int[cells];
            Palettes.unpack(indices, packed, bits);
            int i = 0;
            for (int y = 0; y < dimension; y++) {
                for (int z = 0; z < dimension; z++) {
                    for (int x = 0; x < dimension; x++) {
                        int index = indices[i++];
                        if (index >= size)
                            throw new IllegalStateException(
                                "palette index " + index + " of " + size
                            );
                        palette.set(x, y, z, values[index]);
                    }
                }
            }
            return palette;
        }

        private int readBlock(NetworkBuffer buffer, int reference) {
            return readReference(buffer, reference, blockStates, this::resolveBlock);
        }

        /// The id a name reference stands for, reading and resolving the name when it's the first reference.
        private static int readReference(
            NetworkBuffer buffer,
            int reference,
            IntArrayList table,
            ToIntFunction<String> resolve
        ) {
            if (reference == table.size()) table.add(resolve.applyAsInt(buffer.read(STRING)));
            else if (reference < 0 || reference > table.size())
                throw new IllegalStateException(
                    "name reference " + reference + " of " + table.size()
                );
            return table.getInt(reference);
        }

        private int resolveBlock(String state) {
            if (dataVersion < gameData.dataVersion())
                state = gameData.upgradeBlockState(dataVersion, gameData.dataVersion(), state);
            @Nullable Block block = Block.fromState(state);
            if (block != null) return block.stateId();
            warn("unknown block state {}, read as air", state);
            return Block.AIR.stateId();
        }

        private int resolveBiome(String name) {
            int id = Key.parseable(name) ? biomeId(Key.key(name)) : -1;
            if (id != -1) return id;
            warn("unknown biome {}, read as plains", name);
            return biomeId(PLAINS);
        }

        private @Nullable TypedData resolveEntity(String type, CompoundBinaryTag nbt) {
            var data = new TypedData(type, nbt);
            if (dataVersion < gameData.dataVersion())
                data = gameData.upgradeEntity(dataVersion, gameData.dataVersion(), data);
            if (Key.parseable(data.type())) return data;
            warn("dropped an entity with an invalid type {}", data.type());
            return null;
        }

        private int biomeId(Key key) {
            RegistryKey<Biome> registryKey = biomes.getKey(key);
            return registryKey != null ? biomes.getId(registryKey) : -1;
        }

        private void warn(String message, String name) {
            if (warned.add(name.toLowerCase(Locale.ROOT))) LOGGER.warn(message, name);
        }

        private static int readCell(NetworkBuffer buffer) {
            int index = buffer.read(SHORT) & 0xFFFF;
            if (index >= BLOCK_DIM * BLOCK_DIM * BLOCK_DIM)
                throw new IllegalStateException("cell index " + index);
            return index;
        }

        private static BlockVec blockVec(Point point) {
            return new BlockVec(point.blockX(), point.blockY(), point.blockZ());
        }
    }

    //endregion
}
