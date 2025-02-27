package net.hollowcube.compat.axiom.data.annotations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.ArrayList;

public record FreehandOutlineAnnotation(
        int x,
        int y,
        int z,

        int count,
        int color,
        ByteList points
) implements AnnotationData {

    public static final NetworkBuffer.Type<FreehandOutlineAnnotation> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.VAR_INT, FreehandOutlineAnnotation::x,
            NetworkBuffer.VAR_INT, FreehandOutlineAnnotation::y,
            NetworkBuffer.VAR_INT, FreehandOutlineAnnotation::z,
            NetworkBuffer.VAR_INT, FreehandOutlineAnnotation::count,
            NetworkBuffer.INT, FreehandOutlineAnnotation::color,
            NetworkBuffer.BYTE_ARRAY.transform(ByteArrayList::new, ByteList::toByteArray), FreehandOutlineAnnotation::points,
            FreehandOutlineAnnotation::new
    );

    private static final Codec<ByteList> BYTE_LIST_CODEC = Codec.BYTE.listOf().xmap(ByteArrayList::new, ArrayList::new);
    public static final MapCodec<FreehandOutlineAnnotation> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(FreehandOutlineAnnotation::x),
            Codec.INT.fieldOf("y").forGetter(FreehandOutlineAnnotation::y),
            Codec.INT.fieldOf("z").forGetter(FreehandOutlineAnnotation::z),
            Codec.INT.fieldOf("count").forGetter(FreehandOutlineAnnotation::count),
            Codec.INT.fieldOf("color").forGetter(FreehandOutlineAnnotation::color),
            BYTE_LIST_CODEC.fieldOf("points").forGetter(FreehandOutlineAnnotation::points)
    ).apply(instance, FreehandOutlineAnnotation::new));
}
