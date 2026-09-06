package net.hollowcube.compat.axiom.data.buffers;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/// Biome cells in quart (4x4x4 block) coordinates, grouped by 16^3-quart chunks keyed like block positions.
/// A cell holding the default value carries no change.
public record AxiomBiomeBuffer(
        @NotNull List<Key> palette,
        byte defaultValue,
        @NotNull Long2ObjectMap<byte[]> chunks
) implements AxiomBuffer {

    private static final long EOD = 0b1000000000000000000000000010000000000000000000000000100000000000L;
    private static final int CHUNK_SIZE = 16 * 16 * 16;
    private static final NetworkBuffer.Type<byte[]> CHUNK = NetworkBuffer.FixedRawBytes(CHUNK_SIZE);

    public static AxiomBiomeBuffer read(NetworkBuffer buffer) {
        int paletteSize = buffer.read(NetworkBuffer.BYTE) & 0xFF;
        var palette = new ArrayList<Key>(paletteSize);
        for (int i = 0; i < paletteSize; i++) {
            palette.add(Key.key(buffer.read(NetworkBuffer.STRING)));
        }

        byte defaultValue = buffer.read(NetworkBuffer.BYTE);
        var chunks = new Long2ObjectArrayMap<byte[]>();
        while (true) {
            long key = buffer.read(NetworkBuffer.LONG);
            if (key == EOD) break;
            chunks.put(key, buffer.read(CHUNK));
        }
        return new AxiomBiomeBuffer(List.copyOf(palette), defaultValue, chunks);
    }

    public void forEach(@NotNull CellConsumer consumer) {
        Long2ObjectMaps.fastForEach(this.chunks, entry -> {
            long key = entry.getLongKey();
            int baseX = unpackX(key) * 16;
            int baseY = unpackY(key) * 16;
            int baseZ = unpackZ(key) * 16;
            byte[] cells = entry.getValue();
            int index = 0;
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        int value = cells[index++] & 0xFF;
                        if (value == (this.defaultValue & 0xFF) || value == 0 || value > this.palette.size()) continue;
                        consumer.accept(baseX + x, baseY + y, baseZ + z, this.palette.get(value - 1));
                    }
                }
            }
        });
    }

    @FunctionalInterface
    public interface CellConsumer {
        void accept(int quartX, int quartY, int quartZ, @NotNull Key biome);
    }

    private static int unpackX(long key) {
        return (int) (key >> 38);
    }

    private static int unpackY(long key) {
        return (int) ((key << 52) >> 52);
    }

    private static int unpackZ(long key) {
        return (int) ((key << 26) >> 38);
    }
}
