package net.hollowcube.common.util.font;

import it.unimi.dsi.fastutil.ints.*;
import net.hollowcube.common.util.NetworkBufferTypes;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;

@NotNullByDefault
public class FontWidthMap implements Int2IntFunction {

    public static final NetworkBuffer.Type<FontWidthMap> NETWORK_TYPE = NetworkBufferTypes.readOnly(buffer -> {
        int fallbackWidth = buffer.read(NetworkBuffer.VAR_INT);
        int size = buffer.read(NetworkBuffer.VAR_INT);
        Int2ObjectMap<IntList> widths = new Int2ObjectArrayMap<>();
        for (int i = 0; i < size; i++) {
            int width = buffer.read(NetworkBuffer.VAR_INT);
            int charCount = buffer.read(NetworkBuffer.VAR_INT);
            IntList chars = new IntArrayList(charCount);
            for (int j = 0; j < charCount; j++) {
                int codepoint = buffer.read(NetworkBuffer.VAR_INT);
                chars.add(codepoint);
            }
            widths.put(width, chars);
        }
        return new FontWidthMap(fallbackWidth, widths);
    });

    private final byte[] widths;
    private final int fallback;

    private FontWidthMap(int fallback, Int2ObjectMap<IntList> widths) {
        this.fallback = fallback;
        this.widths = new byte[widths.values().stream().flatMapToInt(IntList::intStream).max().orElseThrow() / 2 + 1];

        for (var entry : widths.int2ObjectEntrySet()) {
            int width = entry.getIntKey();
            for (int charCode : entry.getValue()) {
                int index = charCode / 2;
                if (charCode % 2 == 0) {
                    this.widths[index] = (byte) (this.widths[index] & 0xF0 | width & 0x0F);
                } else {
                    this.widths[index] = (byte) (this.widths[index] & 0x0F | (width & 0x0F) << 4);
                }
            }
        }
    }

    @Override
    public int get(int codepoint) {
        if (codepoint < 0 || codepoint >= this.widths.length * 2) {
            return this.fallback;
        }
        int index = codepoint / 2;
        int width = this.widths[index] & 0x0F;
        if (codepoint % 2 == 1) {
            width = (this.widths[index] >> 4) & 0x0F;
        }
        return width == 0 ? this.fallback : width;
    }

    public static FontWidthMap loadFromResources(String path) {
        try (var is = FontWidthMap.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Font width map not found: " + path);
            }
            return NETWORK_TYPE.read(NetworkBuffer.wrap(is.readAllBytes(), 0, 0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
