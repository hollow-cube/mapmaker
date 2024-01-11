package net.hollowcube.terraform.buffer;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.hollowcube.terraform.buffer.palette.Palette;
import net.hollowcube.terraform.util.PaletteUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class BaseBufferBuilderTest {

    abstract @NotNull BlockBuffer.Builder createBuilder();

    @Test
    void testSingleSection() {
        var buffer = createBuilder();
        buffer.set(0, 0, 0, 1);
        var actual = collect(buffer.build());

        assertEquals(1, actual.size());
        assertTrue(actual.containsKey(PaletteUtil.packPos(0, 0, 0)));
    }

    @Test
    void testMultiSection() {
        var buffer = createBuilder();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    buffer.set(x * 16, y * 16, z * 16, 1);
                }
            }
        }
        var actual = collect(buffer.build());

        assertEquals(125, actual.size());
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    assertTrue(actual.containsKey(PaletteUtil.packPos(x, y, z)));
                }
            }
        }
    }

    private @NotNull Long2ObjectMap<Palette> collect(@NotNull BlockBuffer buffer) {
        var result = new Long2ObjectArrayMap<Palette>();
        buffer.forEachSection((x, y, z, palette) -> {
            result.put(PaletteUtil.packPos(x, y, z), palette);
        });
        return result;
    }
}
