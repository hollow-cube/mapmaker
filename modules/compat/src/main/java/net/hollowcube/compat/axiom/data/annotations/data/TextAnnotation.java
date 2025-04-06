package net.hollowcube.compat.axiom.data.annotations.data;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record TextAnnotation(
        @NotNull String text,

        float x,
        float y,
        float z,

        float rotX,
        float rotY,
        float rotZ,
        float rotW,

        byte direction,
        float fallbackYaw,
        float scale,
        byte billboard,
        int color,
        boolean shadow
) implements AnnotationData {

    public static final NetworkBuffer.Type<TextAnnotation> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, TextAnnotation::text,
            NetworkBuffer.FLOAT, TextAnnotation::x,
            NetworkBuffer.FLOAT, TextAnnotation::y,
            NetworkBuffer.FLOAT, TextAnnotation::z,
            NetworkBuffer.FLOAT, TextAnnotation::rotX,
            NetworkBuffer.FLOAT, TextAnnotation::rotY,
            NetworkBuffer.FLOAT, TextAnnotation::rotZ,
            NetworkBuffer.FLOAT, TextAnnotation::rotW,
            NetworkBuffer.BYTE, TextAnnotation::direction,
            NetworkBuffer.FLOAT, TextAnnotation::fallbackYaw,
            NetworkBuffer.FLOAT, TextAnnotation::scale,
            NetworkBuffer.BYTE, TextAnnotation::billboard,
            NetworkBuffer.INT, TextAnnotation::color,
            NetworkBuffer.BOOLEAN, TextAnnotation::shadow,
            TextAnnotation::new);
    public static final StructCodec<TextAnnotation> CODEC = StructCodec.struct(
            "text", Codec.STRING, TextAnnotation::text,
            "x", Codec.FLOAT, TextAnnotation::x,
            "y", Codec.FLOAT, TextAnnotation::y,
            "z", Codec.FLOAT, TextAnnotation::z,
            "rotX", Codec.FLOAT, TextAnnotation::rotX,
            "rotY", Codec.FLOAT, TextAnnotation::rotY,
            "rotZ", Codec.FLOAT, TextAnnotation::rotZ,
            "rotW", Codec.FLOAT, TextAnnotation::rotW,
            "direction", Codec.BYTE, TextAnnotation::direction,
            "fallbackYaw", Codec.FLOAT, TextAnnotation::fallbackYaw,
            "scale", Codec.FLOAT, TextAnnotation::scale,
            "billboard", Codec.BYTE, TextAnnotation::billboard,
            "color", Codec.INT, TextAnnotation::color,
            "shadow", Codec.BOOLEAN, TextAnnotation::shadow,
            TextAnnotation::new);

    @Override
    public AnnotationData withPosition(float x, float y, float z) {
        return new TextAnnotation(text, x, y, z, rotX, rotY, rotZ, rotW, direction, fallbackYaw, scale, billboard, color, shadow);
    }

    @Override
    public AnnotationData withRotation(float x, float y, float z, float w) {
        return new TextAnnotation(text, this.x, this.y, this.z, x, y, z, w, direction, fallbackYaw, scale, billboard, color, shadow);
    }
}
