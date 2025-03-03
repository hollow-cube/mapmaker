package net.hollowcube.compat.axiom.data.annotations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
            BoxOutlineAnnotation::new
    );

    public static final MapCodec<BoxOutlineAnnotation> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("minX").forGetter(BoxOutlineAnnotation::minX),
            Codec.INT.fieldOf("minY").forGetter(BoxOutlineAnnotation::minY),
            Codec.INT.fieldOf("minZ").forGetter(BoxOutlineAnnotation::minZ),
            Codec.INT.fieldOf("maxX").forGetter(BoxOutlineAnnotation::maxX),
            Codec.INT.fieldOf("maxY").forGetter(BoxOutlineAnnotation::maxY),
            Codec.INT.fieldOf("maxZ").forGetter(BoxOutlineAnnotation::maxZ),
            Codec.INT.fieldOf("color").forGetter(BoxOutlineAnnotation::color)
    ).apply(instance, BoxOutlineAnnotation::new));
}
