package net.hollowcube.terraform.schem;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.nbt.NbtStreamReader;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.block.BlockManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static net.hollowcube.terraform.nbt.NbtTypes.*;

/// Reads a Sponge `.schem` (versions 1, 2 and 3) into a [Schematic] with origin `(0, 0, 0)` and each
/// cell at `Offset + position`.
public final class SpongeSchematicReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpongeSchematicReader.class);

    private static final int MIN_VERSION = 1;
    private static final int MAX_VERSION = 3;
    private static final int UNSET = -1;

    // Block state ids fit an unsigned short, so a palette never needs more entries or a higher id.
    private static final int MAX_PALETTE_ENTRIES = 1 << 16;
    private static final int MAX_PALETTE_ID = MAX_PALETTE_ENTRIES - 1;
    private static final int MAX_LOGGED_UNKNOWN_STATES = 16;

    public static Schematic read(byte[] data, GameDataProvider gameData) throws IOException {
        return new SpongeSchematicReader(gameData).read(data);
    }

    private final GameDataProvider gameData;
    private final Set<String> unknownStates = new HashSet<>();

    private int version = UNSET;
    private int dataVersion = UNSET;
    private int width = UNSET, height = UNSET, length = UNSET;
    private int @Nullable [] offsetTag;
    private @Nullable CompoundBinaryTag metadata; // null until read
    private String @Nullable [] palette; // state strings by palette id, gaps null

    // Block data the first pass skipped because something the decode needs came after it.
    private boolean blockDataDeferred;
    private boolean deferredInBlocks; // in the v3 `Blocks` container, else at the schematic's top level

    private BlockVec offset = BlockVec.ZERO;
    private EditBuffer.@Nullable Builder out;
    private final List<CapturedBlockEntity> blockEntities = new ArrayList<>();

    private SpongeSchematicReader(GameDataProvider gameData) {
        this.gameData = gameData;
    }

    private Schematic read(byte[] data) throws IOException {
        try (NbtStreamReader nbt = enterSchematic(data)) {
            parseSchematicBody(nbt);
        }
        if (blockDataDeferred) {
            try (NbtStreamReader nbt = enterSchematic(data)) {
                seekDeferredBlockData(nbt);
            }
        }
        if (out == null) throw new IOException("schematic has no block data");
        applyBlockEntities(out);
        return new Schematic(
            out.build(),
            BlockVec.ZERO,
            metadata != null ? metadata : CompoundBinaryTag.empty()
        );
    }

    /// Dispatches the schematic compound's tags, for the v1/v2 and v3 layouts alike.
    private void parseSchematicBody(NbtStreamReader nbt) throws IOException {
        // v1/v2 keep `Palette`, `BlockData` and the block entities at the top level, v3 nests them in `Blocks`.
        byte t;
        while ((t = nbt.readType()) != END) {
            String key = nbt.readString();
            switch (key) {
                case "Version" -> {
                    requireType(key, t, INT);
                    version = nbt.readInt();
                    if (version < MIN_VERSION || version > MAX_VERSION)
                        throw new IOException("unsupported schematic version " + version);
                }
                case "DataVersion" -> {
                    requireType(key, t, INT);
                    dataVersion = nbt.readInt();
                }
                case "Width" -> width = readDimension(nbt, key, t);
                case "Height" -> height = readDimension(nbt, key, t);
                case "Length" -> length = readDimension(nbt, key, t);
                case "Offset" -> offsetTag = readPosition(nbt, key, t);
                case "Metadata" -> {
                    requireType(key, t, COMPOUND);
                    metadata = (CompoundBinaryTag) nbt.readPayload(t);
                }
                case "Palette" -> readPalette(nbt, t);
                case "BlockData" -> handleBlockData(nbt, t, false);
                case "TileEntities", "BlockEntities" -> readBlockEntities(nbt, key, t, true);
                case "Blocks" -> readBlocksV3(nbt, t);
                // PaletteMax is only a hint (the palette is sized from its ids), biomes and entities are
                // not read.
                default -> nbt.skipPayload(t);
            }
        }
    }

    /// The v3 `Blocks` compound: `Palette`, `Data`, `BlockEntities`.
    private void readBlocksV3(NbtStreamReader nbt, byte type) throws IOException {
        requireType("Blocks", type, COMPOUND);
        byte t;
        while ((t = nbt.readType()) != END) {
            String key = nbt.readString();
            switch (key) {
                case "Palette" -> readPalette(nbt, t);
                case "Data" -> handleBlockData(nbt, t, true);
                case "BlockEntities" -> readBlockEntities(nbt, key, t, false);
                default -> nbt.skipPayload(t);
            }
        }
    }

    /// Captures the palette's state strings by id. They resolve at decode time, which needs the
    /// `DataVersion`, a tag a hash-ordered writer may put after the palette.
    private void readPalette(NbtStreamReader nbt, byte type) throws IOException {
        requireType("Palette", type, COMPOUND);
        String[] strings = new String[16];
        int maxId = -1;
        int entries = 0;
        byte t;
        while ((t = nbt.readType()) != END) {
            if (++entries > MAX_PALETTE_ENTRIES)
                throw new IOException(
                    "block palette has more than " + MAX_PALETTE_ENTRIES + " entries"
                );
            String state = nbt.readString();
            requireType(state, t, INT);
            int id = nbt.readInt();
            if (id < 0 || id > MAX_PALETTE_ID)
                throw new IOException("palette id out of range: " + id);
            if (id >= strings.length)
                strings = Arrays.copyOf(strings, Math.max(id + 1, strings.length * 2));
            strings[id] = state;
            if (id > maxId) maxId = id;
        }
        if (maxId < 0) throw new IOException("empty block palette");
        palette = Arrays.copyOf(strings, maxId + 1);
    }

    /// The palette as state ids, upgraded from the schematic's `DataVersion`. Ids the palette never
    /// names resolve to air.
    private int[] resolvePalette() {
        String[] strings = Objects.requireNonNull(palette);
        int[] states = new int[strings.length];
        for (int i = 0; i < strings.length; i++) {
            @Nullable String state = strings[i];
            states[i] = state == null ? Block.AIR.stateId() : resolveBlock(state);
        }
        return states;
    }

    private int resolveBlock(String state) {
        String upgraded = dataVersion < gameData.dataVersion()
            ? gameData.upgradeBlockState(dataVersion, gameData.dataVersion(), state)
            : state;
        Block block = Block.fromState(upgraded);
        if (block != null) return block.stateId();

        if (unknownStates.size() < MAX_LOGGED_UNKNOWN_STATES && unknownStates.add(state)) {
            LOGGER.warn(
                "unknown block state '{}' in a schematic (DataVersion {}), reading it as air",
                state,
                dataVersion
            );
        }
        return Block.AIR.stateId();
    }

    /// Streams the block-data varints into the buffer, or defers them to the second pass when
    /// something the decode needs has not been read yet ([#canStreamNow()]).
    private void handleBlockData(
        NbtStreamReader nbt,
        byte type,
        boolean inBlocks
    ) throws IOException {
        requireType(inBlocks ? "Data" : "BlockData", type, BYTE_ARRAY);
        int len = nbt.readLength();
        if (canStreamNow()) {
            decodeBlockData(nbt, len);
        } else {
            nbt.skipBytes(len);
            blockDataDeferred = true;
            deferredInBlocks = inBlocks;
        }
    }

    /// Whether everything the decode needs is already known. A tag a writer puts after the block data
    /// (a hash-ordered writer puts `Width` and `Offset` last) would otherwise be missed. Optional tags
    /// only count when every relevant writer emits them, so a schematic without them still decodes, on
    /// the second pass.
    private boolean canStreamNow() {
        return version != UNSET
            && dataVersion != UNSET
            && dimensionsKnown()
            && palette != null
            && offsetTag != null
            && (version >= 3 || metadata != null);
    }

    /// The second pass: seeks to the block data the first pass skipped and streams it. A schematic
    /// with no `DataVersion` decodes from version 0, so every upgrade runs over it.
    private void seekDeferredBlockData(NbtStreamReader nbt) throws IOException {
        if (palette == null) throw new IOException("block data present but no palette");
        if (!dimensionsKnown()) throw new IOException("block data present but no dimensions");
        if (version == UNSET) throw new IOException("missing schematic version");
        if (dataVersion == UNSET) dataVersion = 0;

        byte t;
        if (deferredInBlocks) {
            if (seek(nbt, "Blocks") != COMPOUND)
                throw new IOException("Blocks compound vanished between passes");
            t = seek(nbt, "Data");
        } else {
            t = seek(nbt, "BlockData");
        }
        if (t != BYTE_ARRAY) throw new IOException("block data vanished between passes");
        decodeBlockData(nbt, nbt.readLength());
    }

    /// Decodes `totalBytes` of palette-id varints into the buffer in Sponge order (x fastest, then z,
    /// then y).
    private void decodeBlockData(NbtStreamReader nbt, int totalBytes) throws IOException {
        int[] states = resolvePalette();
        offset = resolveOffset();
        long declared = declaredCells();
        if (out == null) out = EditBuffer.builder();
        EditBuffer.Builder builder = out;
        int w = width, l = length;
        int ox = offset.blockX(), oy = offset.blockY(), oz = offset.blockZ();

        int x = 0, z = 0, y = 0;
        long cell = 0;
        int acc = 0, shift = 0;
        byte[] chunk = new byte[Math.min(1 << 16, Math.max(1, totalBytes))];
        int remaining = totalBytes;
        while (remaining > 0) {
            int n = Math.min(chunk.length, remaining);
            nbt.readFully(chunk, 0, n);
            remaining -= n;
            for (int i = 0; i < n; i++) {
                int b = chunk[i] & 0xFF;
                acc |= (b & 0x7F) << shift;
                if ((b & 0x80) != 0) {
                    shift += 7;
                    if (shift > 28) throw new IOException("malformed varint in block data");
                    continue;
                }
                if (cell >= declared)
                    throw new IOException("block data exceeds declared dimensions");
                if (acc < 0 || acc >= states.length)
                    throw new IOException("palette id out of range: " + acc);
                builder.setBlockState(ox + x, oy + y, oz + z, states[acc]);
                acc = 0;
                shift = 0;
                cell++;
                if (++x == w) {
                    x = 0;
                    if (++z == l) {
                        z = 0;
                        y++;
                    }
                }
            }
        }
        if (shift != 0) throw new IOException("truncated varint in block data");
        if (cell != declared)
            throw new IOException(
                "block data shorter than dimensions (" + cell + " of " + declared + ")"
            );
    }

    /// Re-attaches each block entity's data onto its decoded cell, with the handler registered under
    /// its id when there is one.
    private void applyBlockEntities(EditBuffer.Builder builder) {
        BlockManager blockManager = MinecraftServer.getBlockManager();
        for (CapturedBlockEntity be : blockEntities) {
            int ax = offset.blockX() + be.x,
                ay = offset.blockY() + be.y,
                az = offset.blockZ() + be.z;
            int stateId = builder.getBlockState(ax, ay, az);
            if (stateId == EditBuffer.NONE) continue; // outside the decoded volume

            var data = new GameDataProvider.TypedData(be.id, be.nbt);
            if (dataVersion < gameData.dataVersion()) {
                data = gameData.upgradeBlockEntity(dataVersion, gameData.dataVersion(), data);
            }
            // Legacy writers capitalized block-entity ids ("Beacon"), handlers are keyed lower-case.
            String id = data.type().toLowerCase(Locale.ROOT);
            if (id.indexOf(':') < 0) id = "minecraft:" + id;
            Block block = Block.fromStateId(stateId);
            @Nullable BlockHandler handler = blockManager.getHandler(id);
            if (handler != null) block = block.withHandler(handler);
            if (data.nbt().size() > 0) block = block.withNbt(data.nbt());
            builder.setBlockState(ax, ay, az, block);
        }
    }

    /// The block-entity list, each entry with an `Id` and a `Pos`. v3 nests the entity's data in a
    /// `Data` compound, while v1/v2 (`inlineFields`) spread it over the entry's other fields
    /// (`ContentVersion` excluded).
    private void readBlockEntities(
        NbtStreamReader nbt,
        String key,
        byte type,
        boolean inlineFields
    ) throws IOException {
        requireType(key, type, LIST);
        byte elem = nbt.readType();
        int len = nbt.readListLength(elem);
        if (len > 0) requireType(key + " element", elem, COMPOUND);
        for (int i = 0; i < len; i++) {
            @Nullable String id = null;
            int @Nullable [] pos = null;
            @Nullable CompoundBinaryTag data = null;
            CompoundBinaryTag.Builder inline = CompoundBinaryTag.builder();
            byte t;
            while ((t = nbt.readType()) != END) {
                String field = nbt.readString();
                switch (field) {
                    case "Id" -> {
                        requireType(field, t, STRING);
                        id = nbt.readString();
                    }
                    case "Pos" -> pos = readPosition(nbt, field, t);
                    case "ContentVersion" -> nbt.skipPayload(t);
                    default -> {
                        if (inlineFields) {
                            inline.put(field, nbt.readPayload(t));
                        } else if (field.equals("Data")) {
                            requireType(field, t, COMPOUND);
                            data = (CompoundBinaryTag) nbt.readPayload(t);
                        } else {
                            nbt.skipPayload(t);
                        }
                    }
                }
            }
            if (id == null) throw new IOException("block entity without an Id");
            if (pos == null) throw new IOException("block entity without a Pos");
            CompoundBinaryTag nbtData = inlineFields
                ? inline.build()
                : data != null ? data : CompoundBinaryTag.empty();
            blockEntities.add(new CapturedBlockEntity(pos[0], pos[1], pos[2], id, nbtData));
        }
    }

    private boolean dimensionsKnown() {
        return width > 0 && height > 0 && length > 0;
    }

    private long declaredCells() throws IOException {
        try {
            return Math.multiplyExact(Math.multiplyExact((long) width, height), length);
        } catch (ArithmeticException e) {
            throw new IOException("dimensions overflow: " + width + "x" + height + "x" + length, e);
        }
    }

    // v1/v2 kept the paste-relative offset in Metadata's WEOffset while Offset held the world position.
    // v3 made Offset itself paste-relative, so WEOffset wins only below v3.
    private BlockVec resolveOffset() {
        if (version < 3 && metadata != null && metadata.get("WEOffsetX") != null) {
            return new BlockVec(
                metadata.getInt("WEOffsetX"),
                metadata.getInt("WEOffsetY"),
                metadata.getInt("WEOffsetZ")
            );
        }
        if (offsetTag != null) return new BlockVec(offsetTag[0], offsetTag[1], offsetTag[2]);
        return BlockVec.ZERO;
    }

    private static NbtStreamReader open(byte[] data) throws IOException {
        return new NbtStreamReader(new GZIPInputStream(new ByteArrayInputStream(data)));
    }

    /// Opens `data` positioned inside the schematic compound: a root named `Schematic` (v1/v2), the
    /// `Schematic` compound nested in the root (v3), or else the root itself (v1/v2 written with an
    /// unnamed root, which the spec allows).
    private static NbtStreamReader enterSchematic(byte[] data) throws IOException {
        NbtStreamReader nbt = open(data);
        try {
            if (nbt.readType() != COMPOUND) throw new IOException("root tag is not a compound");
            if (nbt.readString().equals("Schematic") || seek(nbt, "Schematic") == COMPOUND)
                return nbt;
        } catch (IOException | RuntimeException e) {
            nbt.close();
            throw e;
        }
        nbt.close();

        NbtStreamReader root = open(data);
        root.readType();
        root.readString();
        return root;
    }

    /// Advances through the current compound to the named tag, skipping everything before it, and
    /// returns its type, or `END` when the compound ends without it.
    private static byte seek(NbtStreamReader nbt, String key) throws IOException {
        byte t;
        while ((t = nbt.readType()) != END) {
            if (nbt.readString().equals(key)) return t;
            nbt.skipPayload(t);
        }
        return END;
    }

    /// The format stores dimensions as unsigned shorts.
    private static int readDimension(
        NbtStreamReader nbt,
        String key,
        byte type
    ) throws IOException {
        requireType(key, type, SHORT);
        return nbt.readShort() & 0xFFFF;
    }

    /// An `int[3]` position.
    private static int[] readPosition(
        NbtStreamReader nbt,
        String key,
        byte type
    ) throws IOException {
        requireType(key, type, INT_ARRAY);
        int[] position = nbt.readIntArray();
        if (position.length != 3)
            throw new IOException("'%s' has %d values, expected 3".formatted(key, position.length));
        return position;
    }

    private static void requireType(String key, byte actual, byte expected) throws IOException {
        if (actual != expected) {
            throw new IOException(
                "'%s' has tag type %d, expected %d".formatted(key, actual, expected)
            );
        }
    }

    /// A block entity at its schematic-local position.
    private record CapturedBlockEntity(int x, int y, int z, String id, CompoundBinaryTag nbt) {}
}
