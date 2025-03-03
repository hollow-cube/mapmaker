package net.hollowcube.compat.axiom.data.annotations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record ImageAnnotation(
        @NotNull String url,

        float x,
        float y,
        float z,

        float rotX,
        float rotY,
        float rotZ,
        float rotW,

        byte direction,
        float fallbackYaw,
        float width,
        float opacity,
        byte billboard
) implements AnnotationData {

    public static final NetworkBuffer.Type<ImageAnnotation> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, ImageAnnotation::url,
            NetworkBuffer.FLOAT, ImageAnnotation::x,
            NetworkBuffer.FLOAT, ImageAnnotation::y,
            NetworkBuffer.FLOAT, ImageAnnotation::z,
            NetworkBuffer.FLOAT, ImageAnnotation::rotX,
            NetworkBuffer.FLOAT, ImageAnnotation::rotY,
            NetworkBuffer.FLOAT, ImageAnnotation::rotZ,
            NetworkBuffer.FLOAT, ImageAnnotation::rotW,
            NetworkBuffer.BYTE, ImageAnnotation::direction,
            NetworkBuffer.FLOAT, ImageAnnotation::fallbackYaw,
            NetworkBuffer.FLOAT, ImageAnnotation::width,
            NetworkBuffer.FLOAT, ImageAnnotation::opacity,
            NetworkBuffer.BYTE, ImageAnnotation::billboard,
            ImageAnnotation::new
    );

    public static final MapCodec<ImageAnnotation> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("url").forGetter(ImageAnnotation::url),
            Codec.FLOAT.fieldOf("x").forGetter(ImageAnnotation::x),
            Codec.FLOAT.fieldOf("y").forGetter(ImageAnnotation::y),
            Codec.FLOAT.fieldOf("z").forGetter(ImageAnnotation::z),
            Codec.FLOAT.fieldOf("rotX").forGetter(ImageAnnotation::rotX),
            Codec.FLOAT.fieldOf("rotY").forGetter(ImageAnnotation::rotY),
            Codec.FLOAT.fieldOf("rotZ").forGetter(ImageAnnotation::rotZ),
            Codec.FLOAT.fieldOf("rotW").forGetter(ImageAnnotation::rotW),
            Codec.BYTE.fieldOf("direction").forGetter(ImageAnnotation::direction),
            Codec.FLOAT.fieldOf("fallbackYaw").forGetter(ImageAnnotation::fallbackYaw),
            Codec.FLOAT.fieldOf("width").forGetter(ImageAnnotation::width),
            Codec.FLOAT.fieldOf("opacity").forGetter(ImageAnnotation::opacity),
            Codec.BYTE.fieldOf("billboard").forGetter(ImageAnnotation::billboard)
    ).apply(instance, ImageAnnotation::new));

    @Override
    public AnnotationData withPosition(float x, float y, float z) {
        return new ImageAnnotation(url, x, y, z, rotX, rotY, rotZ, rotW, direction, fallbackYaw, width, opacity, billboard);
    }

    @Override
    public AnnotationData withRotation(float x, float y, float z, float w) {
        return new ImageAnnotation(url, this.x, this.y, this.z, x, y, z, w, direction, fallbackYaw, width, opacity, billboard);
    }
}
