package net.hollowcube.compat.axiom.data.annotations.data;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
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
        FreehandOutlineAnnotation::new);
    private static final Codec<ByteList> BYTE_LIST_CODEC = Codec.BYTE.list().transform(ByteArrayList::new, ArrayList::new);
    public static final StructCodec<FreehandOutlineAnnotation> CODEC = StructCodec.struct(
        "x", Codec.INT, FreehandOutlineAnnotation::x,
        "y", Codec.INT, FreehandOutlineAnnotation::y,
        "z", Codec.INT, FreehandOutlineAnnotation::z,
        "count", Codec.INT, FreehandOutlineAnnotation::count,
        "color", Codec.INT, FreehandOutlineAnnotation::color,
        "points", BYTE_LIST_CODEC, FreehandOutlineAnnotation::points,
        FreehandOutlineAnnotation::new);
}
