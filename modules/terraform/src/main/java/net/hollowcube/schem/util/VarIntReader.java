package net.hollowcube.schem.util;

import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import org.jetbrains.annotations.NotNull;

// Basically just exists to avoid calling #value() on the tag which copies the underlying array
public class VarIntReader {
    private final ByteArrayBinaryTag data;
    private int index = 0;

    public VarIntReader(@NotNull ByteArrayBinaryTag data) {
        this.data = data;
    }

    public int next() {
        // https://github.com/jvm-profiling-tools/async-profiler/blob/a38a375dc62b31a8109f3af97366a307abb0fe6f/src/converter/one/jfr/JfrReader.java#L393
        int result = 0;
        for (int shift = 0; ; shift += 7) {
            byte b = data.get(index++);
            result |= (b & 0x7f) << shift;
            if (b >= 0) {
                return result;
            }
        }
    }
}
