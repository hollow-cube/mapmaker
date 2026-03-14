package net.hollowcube.compat.axiom.data.annotations.data;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.ArrayList;

public record LineAnnotation(
    int x,
    int y,
    int z,
    float width,
    int color,
    ByteList offsets
) implements AnnotationData {

    public static final NetworkBuffer.Type<LineAnnotation> SERIALIZER = NetworkBufferTemplate.template(
        NetworkBuffer.VAR_INT, LineAnnotation::x,
        NetworkBuffer.VAR_INT, LineAnnotation::y,
        NetworkBuffer.VAR_INT, LineAnnotation::z,
        NetworkBuffer.FLOAT, LineAnnotation::width,
        NetworkBuffer.INT, LineAnnotation::color,
        NetworkBuffer.BYTE_ARRAY.transform(ByteArrayList::new, ByteCollection::toByteArray), LineAnnotation::offsets,
        LineAnnotation::new);
    private static final Codec<ByteList> BYTE_LIST_CODEC = Codec.BYTE.list().transform(ByteArrayList::new, ArrayList::new);
    public static final StructCodec<LineAnnotation> CODEC = StructCodec.struct(
        "x", Codec.INT, LineAnnotation::x,
        "y", Codec.INT, LineAnnotation::y,
        "z", Codec.INT, LineAnnotation::z,
        "width", Codec.FLOAT, LineAnnotation::width,
        "color", Codec.INT, LineAnnotation::color,
        "offsets", BYTE_LIST_CODEC, LineAnnotation::offsets,
        LineAnnotation::new);

}
