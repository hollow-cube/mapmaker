package net.hollowcube.terraform.compat.axiom.world.annotation;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.NetworkBufferTypes;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

public sealed interface AnnotationData {
    NetworkBuffer.Type<AnnotationData> NETWORK_TYPE = NetworkBuffer.BYTE
            .unionType(AnnotationData::networkType, AnnotationData::typeId);

    record Line(
            @NotNull Point startQuantized, float lineWidth,
            @NotNull RGBLike color, byte @NotNull [] offsets
    ) implements AnnotationData {
        private static final byte ID = 0;

        public static final NetworkBuffer.Type<Line> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBufferTypes.VECTOR3VI, Line::startQuantized,
                NetworkBuffer.FLOAT, Line::lineWidth,
                Color.NETWORK_TYPE, Line::color,
                NetworkBuffer.BYTE_ARRAY, Line::offsets,
                Line::new);
    }

    record Text(
            @NotNull String text, @NotNull Point position, @NotNull Quaternion rotation,
            @NotNull Direction direction, float fallbackYaw, float scale, byte billboardMode,
            @NotNull RGBLike color, boolean shadow
    ) implements AnnotationData {
        private static final byte ID = 1;

        public static final NetworkBuffer.Type<Text> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.STRING, Text::text,
                NetworkBuffer.VECTOR3, Text::position,
                Quaternion.FLOAT_NETWORK_TYPE, Text::rotation,
                NetworkBuffer.DIRECTION, Text::direction,
                NetworkBuffer.FLOAT, Text::fallbackYaw,
                NetworkBuffer.FLOAT, Text::scale,
                NetworkBuffer.BYTE, Text::billboardMode,
                Color.NETWORK_TYPE, Text::color,
                NetworkBuffer.BOOLEAN, Text::shadow,
                Text::new);
    }

    record Image(
            @NotNull String imageUrl, @NotNull Point position, @NotNull Quaternion rotation,
            @NotNull Direction direction, float fallbackYaw, float width, float opacity,
            byte billboardMode
    ) implements AnnotationData {
        private static final byte ID = 2;

        public static final NetworkBuffer.Type<Image> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.STRING, Image::imageUrl,
                NetworkBuffer.VECTOR3, Image::position,
                Quaternion.FLOAT_NETWORK_TYPE, Image::rotation,
                NetworkBuffer.DIRECTION, Image::direction,
                NetworkBuffer.FLOAT, Image::fallbackYaw,
                NetworkBuffer.FLOAT, Image::width,
                NetworkBuffer.FLOAT, Image::opacity,
                NetworkBuffer.BYTE, Image::billboardMode,
                Image::new);
    }

    record FreehandOutline(
            @NotNull Point start, int offsetCount,
            @NotNull RGBLike color, byte @NotNull [] offsets
    ) implements AnnotationData {
        private static final byte ID = 3;

        public static final NetworkBuffer.Type<FreehandOutline> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBufferTypes.VECTOR3VI, FreehandOutline::start,
                NetworkBuffer.VAR_INT, FreehandOutline::offsetCount,
                Color.NETWORK_TYPE, FreehandOutline::color,
                NetworkBuffer.BYTE_ARRAY, FreehandOutline::offsets,
                FreehandOutline::new);
    }

    record LinesOutline(long @NotNull [] positions, @NotNull RGBLike color) implements AnnotationData {
        private static final byte ID = 4;

        public static final NetworkBuffer.Type<LinesOutline> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBuffer.LONG_ARRAY, LinesOutline::positions,
                Color.NETWORK_TYPE, LinesOutline::color,
                LinesOutline::new);
    }

    record BoxOutline(@NotNull Point from, @NotNull Point to, @NotNull RGBLike color) implements AnnotationData {
        private static final byte ID = 5;

        public static final NetworkBuffer.Type<BoxOutline> NETWORK_TYPE = NetworkBufferTemplate.template(
                NetworkBufferTypes.VECTOR3VI, BoxOutline::from,
                NetworkBufferTypes.VECTOR3VI, BoxOutline::to,
                Color.NETWORK_TYPE, BoxOutline::color,
                BoxOutline::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @NotNull NetworkBuffer.Type<AnnotationData> networkType(byte id) {
        return (NetworkBuffer.Type) switch (id) {
            case 0 -> Line.NETWORK_TYPE;
            case 1 -> Text.NETWORK_TYPE;
            case 2 -> Image.NETWORK_TYPE;
            case 3 -> FreehandOutline.NETWORK_TYPE;
            case 4 -> LinesOutline.NETWORK_TYPE;
            case 5 -> BoxOutline.NETWORK_TYPE;
            default -> throw new IllegalArgumentException("Unknown annotation data type: " + id);
        };
    }

    private static byte typeId(@NotNull AnnotationData data) {
        return switch (data) {
            case Line ignored -> Line.ID;
            case Text ignored -> Text.ID;
            case Image ignored -> Image.ID;
            case FreehandOutline ignored -> FreehandOutline.ID;
            case LinesOutline ignored -> LinesOutline.ID;
            case BoxOutline ignored -> BoxOutline.ID;
        };
    }
}
