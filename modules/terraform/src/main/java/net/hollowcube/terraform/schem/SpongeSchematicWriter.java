package net.hollowcube.terraform.schem;

import net.hollowcube.terraform.buffer.BufferSection;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.nbt.NbtStreamWriter;
import net.hollowcube.terraform.util.BlockUtil;
import net.hollowcube.terraform.util.GameDataProvider;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import static net.hollowcube.terraform.nbt.NbtTypes.COMPOUND;
import static net.hollowcube.terraform.nbt.NbtTypes.END;

/// Writes a [Schematic] as a Sponge schematic v3.
///
/// Unset cells in the buffer are written as air (schematics are dense)
@SuppressWarnings("UnstableApiUsage")
public final class SpongeSchematicWriter {
    private static final String BRAND = "github.com/hollow-cube/terraform";
    private static final int FORMAT_VERSION = 3;
    private static final int MAX_DIMENSION = 0xFFFF; // dimensions are ushorts
    // Block state ids fit an unsigned short.
    private static final int STATE_IDS = 1 << 16;

    public static byte[] write(Schematic schematic, GameDataProvider gameData) {
        var bounds = schematic.buffer().blockBounds();
        if (bounds == null) {
            throw new IllegalArgumentException("cannot save a schematic without blocks");
        }

        var writer = new SpongeSchematicWriter(schematic.buffer(), bounds);
        if (writer.size.blockX() > MAX_DIMENSION
            || writer.size.blockY() > MAX_DIMENSION
            || writer.size.blockZ() > MAX_DIMENSION) {
            throw new IllegalArgumentException(
                "schematic is too large to save: %dx%dx%d (each side at most %d blocks)".formatted(
                    writer.size.blockX(),
                    writer.size.blockY(),
                    writer.size.blockZ(),
                    MAX_DIMENSION
                )
            );
        }
        // Checked before walking the box: every cell takes at least one byte of the block data, whose
        // length is an int.
        if ((long) writer.size.blockX() * writer.size.blockY() * writer.size.blockZ()
            > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("schematic is too large to save");
        }

        var bytes = new ByteArrayOutputStream();
        try (var out = new NbtStreamWriter(new FastGzipOutputStream(bytes))) {
            writer.collectPalette();
            writer.write(out, schematic, gameData.dataVersion());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    private final EditBuffer buffer;
    private final BlockVec min;
    private final BlockVec max;
    private final BlockVec size;
    private final int[] paletteIndex = new int[STATE_IDS];
    private final List<String> palette = new ArrayList<>();
    private long dataBytes;

    private SpongeSchematicWriter(EditBuffer buffer, Area.Cuboid bounds) {
        this.buffer = buffer;
        this.min = bounds.min();
        this.max = bounds.max();
        this.size = bounds.max().sub(bounds.min()).add(1);
        Arrays.fill(paletteIndex, -1);
    }

    /// First pass: numbers every state in first-seen order and sums the varint bytes, since the block
    /// data's length precedes it.
    private void collectPalette() throws IOException {
        forEachCell(stateId -> {
            int index = paletteIndex[stateId];
            if (index == -1) {
                index = palette.size();
                paletteIndex[stateId] = index;
                palette.add(Block.fromStateId(stateId).state());
            }
            dataBytes += VarIntWriter.size(index);
        });
        // Past 127 palette entries a cell takes more than one byte, so the cell count alone does not
        // bound the block data's int length.
        if (dataBytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("schematic is too large to save");
        }
    }

    private void write(
        NbtStreamWriter out,
        Schematic schematic,
        int dataVersion
    ) throws IOException {
        BlockVec origin = schematic.origin();
        out.beginCompound("");
        out.beginCompound("Schematic");

        out.writeInt("Version", FORMAT_VERSION);
        out.writeInt("DataVersion", dataVersion);
        CompoundBinaryTag metadata = schematic.metadata();
        // WorldEdit restores the copied area's world position from this.
        CompoundBinaryTag worldEdit = metadata.getCompound("WorldEdit")
            .putIntArray("Origin", new int[] {origin.blockX(), origin.blockY(), origin.blockZ()});
        out.writeTag(
            "Metadata",
            metadata.putString("Writer", BRAND)
                .putLong("Date", System.currentTimeMillis())
                .put("WorldEdit", worldEdit)
        );
        out.writeShort("Width", size.blockX());
        out.writeShort("Height", size.blockY());
        out.writeShort("Length", size.blockZ());
        out.writeIntArray(
            "Offset",
            min.blockX() - origin.blockX(),
            min.blockY() - origin.blockY(),
            min.blockZ() - origin.blockZ()
        );

        out.beginCompound("Blocks");
        out.beginCompound("Palette");
        for (int i = 0; i < palette.size(); i++) out.writeInt(palette.get(i), i);
        out.endCompound();
        writeBlockData(out);
        writeBlockEntities(out);
        out.endCompound();

        out.endCompound();
        out.endCompound();
    }

    private void writeBlockData(NbtStreamWriter out) throws IOException {
        out.beginByteArray("Data", (int) dataBytes);
        var data = new VarIntWriter(out);
        forEachCell(stateId -> data.write(paletteIndex[stateId]));
        data.flush();
    }

    /// Every block entity in the box. Its id is the handler's key when it has one, else its vanilla
    /// block-entity type. A block with data but neither is written as its plain state.
    private void writeBlockEntities(NbtStreamWriter out) throws IOException {
        record Entry(int x, int y, int z, String id, @Nullable CompoundBinaryTag nbt) {}

        var entries = new ArrayList<Entry>();
        buffer.forEachSection((sx, sy, sz, section) -> {
            int baseX = sx << 4, baseY = sy << 4, baseZ = sz << 4;
            section.forEachBlockEntity((x, y, z, block) -> {
                @Nullable Key id = BlockUtil.blockEntityId(block);
                if (id == null) return;
                entries.add(
                    new Entry(
                        baseX + x - min.blockX(),
                        baseY + y - min.blockY(),
                        baseZ + z - min.blockZ(),
                        id.asString(),
                        block.nbt()
                    )
                );
            });
        });

        out.beginList("BlockEntities", entries.isEmpty() ? END : COMPOUND, entries.size());
        for (Entry entry : entries) {
            out.beginCompound();
            out.writeString("Id", entry.id());
            out.writeIntArray("Pos", entry.x(), entry.y(), entry.z());
            out.writeTag("Data", entry.nbt() != null ? entry.nbt() : CompoundBinaryTag.empty());
            out.endCompound();
        }
    }

    private void forEachCell(CellVisitor consumer) throws IOException {
        for (int y = min.blockY(); y <= max.blockY(); y++) {
            for (int z = min.blockZ(); z <= max.blockZ(); z++) {
                int x = min.blockX();
                while (x <= max.blockX()) {
                    int end = Math.min(max.blockX(), (x | 15));
                    @Nullable BufferSection section = buffer.section(x >> 4, y >> 4, z >> 4);
                    if (section == null) {
                        for (; x <= end; x++) consumer.accept(Block.AIR.stateId());
                    } else {
                        for (; x <= end; x++) {
                            int stateId = section.getBlockState(x & 15, y & 15, z & 15);
                            consumer.accept(
                                stateId == EditBuffer.NONE ? Block.AIR.stateId() : stateId
                            );
                        }
                    }
                }
            }
        }
    }

    @FunctionalInterface
    private interface CellVisitor {
        void accept(int stateId) throws IOException;
    }

    private static final class VarIntWriter {
        private static int size(int value) {
            return value < 1 << 7 ? 1 : value < 1 << 14 ? 2 : value < 1 << 21 ? 3 : 4;
        }

        private final NbtStreamWriter out;
        private final byte[] chunk = new byte[1 << 16];
        private int position;

        VarIntWriter(NbtStreamWriter out) {
            this.out = out;
        }

        void write(int value) throws IOException {
            if (position > chunk.length - 5) flush();
            while ((value & ~0x7F) != 0) {
                chunk[position++] = (byte) ((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            chunk[position++] = (byte) value;
        }

        void flush() throws IOException {
            out.writeBytes(chunk, 0, position);
            position = 0;
        }
    }

    // The default level takes about three times as long for ~10% smaller output.
    private static final class FastGzipOutputStream extends GZIPOutputStream {
        FastGzipOutputStream(OutputStream out) throws IOException {
            super(out, 1 << 16);
            def.setLevel(Deflater.BEST_SPEED);
        }

    }
}
