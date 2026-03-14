package net.hollowcube.compat.axiom.data.annotations.data;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ImageAnnotation(
    String url,

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
        ImageAnnotation::new);
    public static final StructCodec<ImageAnnotation> CODEC = StructCodec.struct(
        "url", Codec.STRING, ImageAnnotation::url,
        "x", Codec.FLOAT, ImageAnnotation::x,
        "y", Codec.FLOAT, ImageAnnotation::y,
        "z", Codec.FLOAT, ImageAnnotation::z,
        "rotX", Codec.FLOAT, ImageAnnotation::rotX,
        "rotY", Codec.FLOAT, ImageAnnotation::rotY,
        "rotZ", Codec.FLOAT, ImageAnnotation::rotZ,
        "rotW", Codec.FLOAT, ImageAnnotation::rotW,
        "direction", Codec.BYTE, ImageAnnotation::direction,
        "fallbackYaw", Codec.FLOAT, ImageAnnotation::fallbackYaw,
        "width", Codec.FLOAT, ImageAnnotation::width,
        "opacity", Codec.FLOAT, ImageAnnotation::opacity,
        "billboard", Codec.BYTE, ImageAnnotation::billboard,
        ImageAnnotation::new);

    @Override
    public AnnotationData withPosition(float x, float y, float z) {
        return new ImageAnnotation(url, x, y, z, rotX, rotY, rotZ, rotW, direction, fallbackYaw, width, opacity, billboard);
    }

    @Override
    public AnnotationData withRotation(float x, float y, float z, float w) {
        return new ImageAnnotation(url, this.x, this.y, this.z, x, y, z, w, direction, fallbackYaw, width, opacity, billboard);
    }
}
