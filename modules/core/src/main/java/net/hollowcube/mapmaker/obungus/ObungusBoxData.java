package net.hollowcube.mapmaker.obungus;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record ObungusBoxData(
        @NotNull String id,
        @Nullable String playerId,
        @NotNull Instant createdAt,
        @Nullable String name,
        @NotNull String shape,
        @Nullable String legacyUsername,
        @NotNull String schematicData
) {
    public static final int FLAG_IS_LONG = 1;
    public static final int FLAG_IS_STRAIGHT = 2;
    public static final int FLAG_IS_RIGHT = 4;
    public static final int FLAG_IS_LONG_RIGHT = 8;

    public static Point SIZE_1_1 = new Vec(15, 19, 15);
    public static Point SIZE_1_2 = new Vec(15, 19, 15 + 3 + 15);

    public @Nullable Point getEndOffset() {
        int shape = bitSetToInt(shape());
        var size = SIZE_1_1;
        if ((shape & FLAG_IS_LONG) != 0) {
            size = SIZE_1_2;

            if ((shape & FLAG_IS_LONG_RIGHT) != 0) {
                return new Vec(7, 3, SIZE_1_2.blockZ() - 1);
            }
        }

        if ((shape & FLAG_IS_RIGHT) != 0)
            return new Vec(0, 3, 7);
        if ((shape & FLAG_IS_STRAIGHT) != 0)
            return new Vec(7, 3, size.blockZ() - 1);
        return null;
    }

    private static int bitSetToInt(String bitSet) {
        if (bitSet == null || bitSet.isEmpty() || !bitSet.matches("[01]+")) {
            throw new IllegalArgumentException("Invalid bit set string: " + bitSet);
        }

        int result = 0;
        for (int i = 0; i < bitSet.length(); i++) {
            char bit = bitSet.charAt(i); // Process from most significant bit
            if (bit == '1') {
                result |= (1 << i); // Set the bit at the corresponding position
            }
        }
        return result;
    }

}
