package net.hollowcube.terraform.compat.axiom.packet.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.common.util.NetworkBufferTypes;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.buffer.palette.FixedPalette;
import net.hollowcube.terraform.buffer.palette.NaivePalette;
import net.hollowcube.terraform.buffer.palette.Palette;
import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.hollowcube.terraform.util.PaletteUtil;
import net.hollowcube.terraform.util.ProtocolUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.palette.Palettes;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minestom.server.network.NetworkBuffer.*;

public record AxiomClientSetBufferPacket(
        @NotNull String dimensionName,
        @NotNull String correlationId,
        boolean isContinuation,
        @Nullable CompoundBinaryTag sourceInfo, // Only present if isContinuation is false
        @NotNull BlockBuffer buffer
) implements AxiomClientPacket {
    private static final Logger logger = LoggerFactory.getLogger(AxiomClientSetBufferPacket.class);

    public static final NetworkBuffer.Type<AxiomClientSetBufferPacket> SERIALIZER = NetworkBufferTypes.readOnly(buffer -> {
        var dimensionName = buffer.read(STRING);
        var correlationId = buffer.read(UUID).toString();
        var isContinuation = buffer.read(BOOLEAN);
        var rawSourceInfo = isContinuation ? null : buffer.read(NBT);
        var sourceInfo = rawSourceInfo instanceof CompoundBinaryTag compound ? compound : null;
        var updateBuffer = switch (buffer.read(BYTE)) {
            case 0 -> buffer.read(AxiomBlockBuffer.SERIALIZER);
            case 1 -> {
                logger.info("received biome buffer but cannot process it correctly :(");
                yield BlockBuffer.empty(); //todo
            }
            default -> throw new IllegalStateException("Unexpected axiom buffer type!");
        };
        return new AxiomClientSetBufferPacket(dimensionName, correlationId, isContinuation, sourceInfo, updateBuffer);
    });

    record AxiomBlockBuffer(@NotNull Long2ObjectMap<Palette> updates) implements BlockBuffer {
        static final NetworkBuffer.Type<AxiomBlockBuffer> SERIALIZER = NetworkBufferTypes.readOnly(buffer -> {
            var updates = new Long2ObjectArrayMap<Palette>(10); // 10 is just an arbitrary number

            for (int i = 0; i < Axiom.MAX_SECTIONS_PER_UPDATE; i++) {
                long index = buffer.read(LONG);
                if (index == ProtocolUtil.MIN_POSITION_LONG) {
                    // Reached the end, exit normally w/o overflow. this is the normal case
                    return new AxiomBlockBuffer(updates);
                }

                var palette = readAxiomPalette(buffer);

                var blockEntityCount = Math.min(4096, buffer.read(VAR_INT));
                for (int j = 0; j < blockEntityCount; j++) {
                    buffer.read(SHORT); // Offset
                    // Begin compressed block entity
                    buffer.read(VAR_INT); // Original size
                    buffer.read(BYTE); // Compression dict
                    buffer.read(BYTE_ARRAY); // Compressed data
                }

                updates.put(index, palette);
            }

            // We can only reach this case if we read more than the max number of sections.
            // In this case read until the end and count how many were overflowed.
            int overflow = 0;
            while (buffer.read(LONG) != ProtocolUtil.MIN_POSITION_LONG) {
                overflow++;

                // Skip the palette
                switch (buffer.read(BYTE)) {
                    case 0 -> buffer.read(VAR_INT); // Fixed
                    case 1, 2, 3, 4, 5, 6, 7, 8 -> { // Linear or HashMap
                        buffer.read(VAR_INT_ARRAY);
                        buffer.read(LONG_ARRAY); // Data
                    }
                    default -> buffer.read(LONG_ARRAY); // Global
                }

                var blockEntityCount = Math.min(4096, buffer.read(VAR_INT));
                for (int j = 0; j < blockEntityCount; j++) {
                    buffer.read(SHORT); // Offset
                    // Begin compressed block entity
                    buffer.read(VAR_INT); // Original size
                    buffer.read(BYTE); // Compression dict
                    buffer.read(BYTE_ARRAY); // Compressed data
                }
            }

            logger.warn("Received {} overflowed sections", overflow);
            return new AxiomBlockBuffer(updates);
        });

        @Override
        public void forEachSection(@NotNull SectionConsumer consumer) {
            for (var entry : updates.long2ObjectEntrySet()) {
                int chunkX = PaletteUtil.unpackX(entry.getLongKey());
                int sectionY = PaletteUtil.unpackY(entry.getLongKey());
                int chunkZ = PaletteUtil.unpackZ(entry.getLongKey());

                consumer.accept(chunkX, sectionY, chunkZ, entry.getValue());
            }
        }

        @Override
        public long sizeBytes() {
            return updates.values().stream()
                    .mapToLong(Palette::sizeBytes)
                    .sum();
        }

        /**
         * Reads an Axiom palette. It is a mojang palette, except that we treat {@link Axiom#EMPTY_BLOCK_STATE} as unset.
         */
        static @NotNull Palette readAxiomPalette(@NotNull NetworkBuffer buffer) {
            var bitsPerEntry = buffer.read(NetworkBuffer.BYTE);
            return switch (bitsPerEntry) {
                case 0 -> { // Vanilla: fixed palette
                    var blockId = buffer.read(NetworkBuffer.VAR_INT);
                    Check.stateCondition(buffer.read(LONG_ARRAY).length != 0, "fixed palette must have zero data");
                    yield new FixedPalette(blockId == Axiom.EMPTY_BLOCK_STATE ? Palette.UNSET : blockId);
                }
                case 1, 2, 3, 4 -> { // Vanilla: Linear palette (always bpe 4)
                    var palette = new NaivePalette(); //todo need to not use this, its bad. NaivePalette should be package private.
                    var paletteEntries = buffer.read(VAR_INT_ARRAY);
                    for (int i = 0; i < paletteEntries.length; i++) { // Convert to our internal representation
                        int blockId = paletteEntries[i];
                        paletteEntries[i] = (blockId == Axiom.EMPTY_BLOCK_STATE ? Palette.UNSET : blockId) + 1;
                    }

                    var paletteData = palette.array();
                    PaletteUtil.unpack(paletteData, buffer.read(LONG_ARRAY), 4);

                    // Replace indices with their actual block ids
                    for (int i = 0; i < paletteData.length; i++) {
                        paletteData[i] = paletteEntries[paletteData[i]];
                    }

                    yield palette;
                }
                case 5, 6, 7, 8 -> { // Vanilla: Hashmap palette (bpe = bits)
                    var palette = new NaivePalette();
                    var paletteEntries = buffer.read(VAR_INT_ARRAY);
                    for (int i = 0; i < paletteEntries.length; i++) { // Convert to our internal representation
                        int blockId = paletteEntries[i];
                        paletteEntries[i] = (blockId == Axiom.EMPTY_BLOCK_STATE ? Palette.UNSET : blockId) + 1;
                    }

                    var paletteData = palette.array();
                    PaletteUtil.unpack(paletteData, buffer.read(LONG_ARRAY), bitsPerEntry);

                    // Replace indices with their actual block ids
                    for (int i = 0; i < paletteData.length; i++) {
                        paletteData[i] = paletteEntries[paletteData[i]];
                    }

                    yield palette;
                }
                default -> { // Vanilla: Global palette (bpe = max)
                    var palette = new NaivePalette();
                    var paletteData = palette.array();
                    Palettes.unpack(paletteData,
                            buffer.read(NetworkBuffer.LONG_ARRAY),
                            PaletteUtil.MAX_BITS_PER_ENTRY);
                    for (int i = 0; i < paletteData.length; i++) {
                        // Convert to our internal representation
                        paletteData[i] = (paletteData[i] == Axiom.EMPTY_BLOCK_STATE ? Palette.UNSET : paletteData[i]) + 1;
                    }
                    yield palette;
                }
            };
        }
    }

}
