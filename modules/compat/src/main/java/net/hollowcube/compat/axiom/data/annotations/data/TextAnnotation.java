package net.hollowcube.compat.axiom.data.annotations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record TextAnnotation(
        String text,

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
            TextAnnotation::new
    );

    public static final MapCodec<TextAnnotation> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("text").forGetter(TextAnnotation::text),
            Codec.FLOAT.fieldOf("x").forGetter(TextAnnotation::x),
            Codec.FLOAT.fieldOf("y").forGetter(TextAnnotation::y),
            Codec.FLOAT.fieldOf("z").forGetter(TextAnnotation::z),
            Codec.FLOAT.fieldOf("rotX").forGetter(TextAnnotation::rotX),
            Codec.FLOAT.fieldOf("rotY").forGetter(TextAnnotation::rotY),
            Codec.FLOAT.fieldOf("rotZ").forGetter(TextAnnotation::rotZ),
            Codec.FLOAT.fieldOf("rotW").forGetter(TextAnnotation::rotW),
            Codec.BYTE.fieldOf("direction").forGetter(TextAnnotation::direction),
            Codec.FLOAT.fieldOf("fallbackYaw").forGetter(TextAnnotation::fallbackYaw),
            Codec.FLOAT.fieldOf("scale").forGetter(TextAnnotation::scale),
            Codec.BYTE.fieldOf("billboard").forGetter(TextAnnotation::billboard),
            Codec.INT.fieldOf("color").forGetter(TextAnnotation::color),
            Codec.BOOL.fieldOf("shadow").forGetter(TextAnnotation::shadow)
    ).apply(instance, TextAnnotation::new));

    @Override
    public AnnotationData withPosition(float x, float y, float z) {
        return new TextAnnotation(text, x, y, z, rotX, rotY, rotZ, rotW, direction, fallbackYaw, scale, billboard, color, shadow);
    }

    @Override
    public AnnotationData withRotation(float x, float y, float z, float w) {
        return new TextAnnotation(text, this.x, this.y, this.z, x, y, z, w, direction, fallbackYaw, scale, billboard, color, shadow);
    }
}
