package net.hollowcube.terraform.util;

import net.hollowcube.terraform.buffer.EditBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static net.hollowcube.terraform.util.BlockUtil.*;
import static org.junit.jupiter.api.Assertions.*;

class BlockUtilTest {

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0",
        "1, 2, 3",
        "-1, -2, -3",
        "17, -64, -339",
        "30000000, 2047, 30000000", // vanilla world limit, build-height top
        "-30000000, -2048, -30000000",
        "33554431, 2047, 33554431", // signed 26-bit / 12-bit max
        "-33554432, -2048, -33554432", // signed 26-bit / 12-bit min
        "33554431, -2048, 0",
    })
    void roundTripsWithinRange(int x, int y, int z) {
        long key = packBlockPos(x, y, z);
        assertEquals(x, blockPosGetX(key));
        assertEquals(y, blockPosGetY(key));
        assertEquals(z, blockPosGetZ(key));
    }

    @ParameterizedTest
    @CsvSource({
        "33554432,0,0",
        "-33554433,0,0",
        "0,2048,0",
        "0,-2049,0",
        "0,0,33554432",
        "0,0,-33554433",
    })
    void rejectsCoordinatesThatWouldAlias(int x, int y, int z) {
        assertThrows(IllegalArgumentException.class, () -> packBlockPos(x, y, z));
    }

    @Test
    void bufferRejectsAliasingSectionCoordinates() {
        var builder = EditBuffer.builder();
        assertThrows(IllegalArgumentException.class, () -> builder.setBlockState(0, 65606, 0, 1));
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.setBlockState(1073741824, 70, 0, 1)
        );
    }

    @Test
    void negativeCoordinatesSignExtend() {
        long key = packBlockPos(-1, -64, -33_554_432);
        assertEquals(-1, blockPosGetX(key), "unpack must sign-extend each field");
        assertEquals(-64, blockPosGetY(key));
        assertEquals(-33_554_432, blockPosGetZ(key));
    }

    @Test
    void axesDoNotAlias() {
        // The same value on different axes must produce distinct keys (field boundaries hold).
        assertNotEquals(packBlockPos(5, 0, 0), packBlockPos(0, 5, 0));
        assertNotEquals(packBlockPos(0, 5, 0), packBlockPos(0, 0, 5));
        // A negative y fills its low field with high bits. It must not bleed into z.
        assertNotEquals(packBlockPos(1, 2047, 1), packBlockPos(1, -2048, 2));
    }
}
