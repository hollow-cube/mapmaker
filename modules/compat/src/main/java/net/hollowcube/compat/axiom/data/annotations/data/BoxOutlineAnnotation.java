package net.hollowcube.compat.axiom.data.annotations.data;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record BoxOutlineAnnotation(
    int minX,
    int minY,
    int minZ,

    int maxX,
    int maxY,
    int maxZ,

    int color
) implements AnnotationData {

    public static final NetworkBuffer.Type<BoxOutlineAnnotation> SERIALIZER = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, BoxOutlineAnnotation::minX,
        NetworkBuffer.VAR_INT, BoxOutlineAnnotation::minY,
        NetworkBuffer.VAR_INT, BoxOutlineAnnotation::minZ,
        NetworkBuffer.VAR_INT, BoxOutlineAnnotation::maxX,
        NetworkBuffer.VAR_INT, BoxOutlineAnnotation::maxY,
        NetworkBuffer.VAR_INT, BoxOutlineAnnotation::maxZ,
        NetworkBuffer.INT, BoxOutlineAnnotation::color,
        BoxOutlineAnnotation::new);
    public static final StructCodec<BoxOutlineAnnotation> CODEC = StructCodec.struct(
        "minX", Codec.INT, BoxOutlineAnnotation::minX,
        "minY", Codec.INT, BoxOutlineAnnotation::minY,
        "minZ", Codec.INT, BoxOutlineAnnotation::minZ,
        "maxX", Codec.INT, BoxOutlineAnnotation::maxX,
        "maxY", Codec.INT, BoxOutlineAnnotation::maxY,
        "maxZ", Codec.INT, BoxOutlineAnnotation::maxZ,
        "color", Codec.INT, BoxOutlineAnnotation::color,
        BoxOutlineAnnotation::new);
}
