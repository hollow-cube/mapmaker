package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.util.PaletteUtil;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSetBufferPacket(
        @NotNull String dimensionName,
        @NotNull String correlationId,
        boolean isContinuation,
        @Nullable NBTCompound sourceInfo, // Only present if isContinuation is false
        @NotNull Buffer buffer
) implements AxiomClientPacket {

    public AxiomClientSetBufferPacket(@NotNull AxiomClientSetBufferPacket other) {
        this(other.dimensionName, other.correlationId, other.isContinuation, other.sourceInfo, other.buffer);
    }

    public AxiomClientSetBufferPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(read(buffer, apiVersion));
    }

    private static @NotNull AxiomClientSetBufferPacket read(@NotNull NetworkBuffer buffer, int apiVersion) {
        var dimensionName = buffer.read(STRING);
        var correlationId = buffer.read(UUID).toString();
        var isContinuation = buffer.read(BOOLEAN);
        var rawSourceInfo = isContinuation ? null : buffer.read(NBT);
        var sourceInfo = rawSourceInfo instanceof NBTCompound ? (NBTCompound) rawSourceInfo : null;
        var updateBuffer = switch (buffer.read(BYTE)) {
            case 0 -> new BlockBuffer(buffer, apiVersion);
            case 1 -> new BiomeBuffer(buffer, apiVersion);
            default -> throw new IllegalStateException("Unexpected axiom buffer type!");
        };
        return new AxiomClientSetBufferPacket(dimensionName, correlationId, isContinuation, sourceInfo, updateBuffer);
    }

    public sealed interface Buffer permits BlockBuffer, BiomeBuffer {
    }

    public record BlockBuffer(
            List<SectionUpdate> updates,
            int overflow
            // Not part of the protocol. Present if there were more updates than are allowed in a single packet.
    ) implements Buffer {

        public record SectionUpdate(
                long index,
                @NotNull Palette palette
        ) {
        }

        private static final long MIN_POSITION_LONG = 0b1000000000000000000000000010000000000000000000000000100000000000L;

        public BlockBuffer(@NotNull BlockBuffer other) {
            this(other.updates, other.overflow);
        }

        public BlockBuffer(@NotNull NetworkBuffer buffer, int apiVersion) {
            this(read(buffer, apiVersion));
        }

        private static @NotNull BlockBuffer read(@NotNull NetworkBuffer buffer, int apiVersion) {
            List<SectionUpdate> updates = new ArrayList<>();

            for (int i = 0; i < Axiom.MAX_SECTIONS_PER_UPDATE; i++) {
                long index = buffer.read(LONG);
                if (index == MIN_POSITION_LONG) {
                    // Reached the end, exit normally w/o overflow. this is the normal case
                    return new BlockBuffer(updates, 0);
                }

                var palette = Palette.read(buffer, apiVersion);
                var blockEntityCount = Math.min(4096, buffer.read(VAR_INT));
                if (blockEntityCount > 0) {
                    System.out.println("BLOCK ENTITY COUNT: " + blockEntityCount);
                }

                updates.add(new SectionUpdate(index, palette));
            }

            // We can only reach this case if we read more than the max number of sections.
            // In this case read until the end and count how many were overflowed.
            int overflow = 0;
            while (buffer.read(LONG) != MIN_POSITION_LONG) {
                overflow++;

                // Skip the palette
                switch (buffer.read(BYTE)) {
                    case 0 -> buffer.read(VAR_INT); // Fixed
                    case 1, 2, 3, 4, 5, 6, 7, 8 -> { // Linear or HashMap
                        buffer.readCollection(VAR_INT); // Palette
                        buffer.read(LONG_ARRAY); // Data
                    }
                    default -> buffer.read(LONG_ARRAY); // Global
                }
            }

            return new BlockBuffer(updates, overflow);
        }
    }

    public sealed interface Palette {
        static @NotNull Palette read(@NotNull NetworkBuffer buffer, int apiVersion) {
            byte bits = buffer.read(BYTE);
            return switch (bits) {
                case 0 -> new Fixed(buffer.read(VAR_INT)); // Vanilla: fixed palette
                case 1, 2, 3, 4 -> { // Vanilla: Linear palette (always bpe 4)
                    var rawPalette = buffer.readCollection(VAR_INT);
                    var palette = new int[rawPalette.size()];
                    for (int i = 0; i < rawPalette.size(); i++) {
                        palette[i] = rawPalette.get(i);
                    }
                    yield new Linear(4, palette, buffer.read(LONG_ARRAY));
                }
                case 5, 6, 7, 8 -> { // Vanilla: Hashmap palette (bpe = bits)
                    var rawPalette = buffer.readCollection(VAR_INT);
                    var palette = new int[rawPalette.size()];
                    for (int i = 0; i < rawPalette.size(); i++) {
                        palette[i] = rawPalette.get(i);
                    }
                    yield new Linear(bits, palette, buffer.read(LONG_ARRAY));
                }
                default -> new Global(buffer.read(LONG_ARRAY)); // Vanilla: Global palette (bpe = max)
            };
        }

        void read(int[] paletteData);

        record Fixed(int value) implements Palette {
            @Override
            public void read(int[] paletteData) {
                Arrays.fill(paletteData, value);
            }
        }

        record Linear(int bitsPerEntry, int[] palette, long[] data) implements Palette {
            @Override
            public void read(int[] paletteData) {
                PaletteUtil.unpack(paletteData, data, bitsPerEntry);
                for (int i = 0; i < paletteData.length; i++) {
                    paletteData[i] = palette[paletteData[i]];
                }
            }
        }

        record Global(long[] data) implements Palette {
            public static final int BITS_PER_ENTRY;

            static {
                int bpe = 0;
                for (short id = 24134; id < Short.MAX_VALUE; id++) {
                    if (Block.fromStateId(id) == null) {
                        bpe = (int) Math.ceil(Math.log(id) / Math.log(2));
                        break;
                    }
                }

                Check.stateCondition(bpe == 0, "Could not find max bits per entry");
                BITS_PER_ENTRY = bpe;
            }

            @Override
            public void read(int[] paletteData) {
                PaletteUtil.unpack(paletteData, data, BITS_PER_ENTRY);
            }
        }
    }

    public record BiomeBuffer() implements Buffer {

        public BiomeBuffer(@NotNull NetworkBuffer buffer, int apiVersion) {
            this();
        }

    }

}
