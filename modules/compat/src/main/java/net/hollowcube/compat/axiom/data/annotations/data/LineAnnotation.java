package net.hollowcube.compat.axiom.data.annotations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.bytes.ByteList;
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
            LineAnnotation::new
    );

    private static final Codec<ByteList> BYTE_LIST_CODEC = Codec.BYTE.listOf().xmap(ByteArrayList::new, ArrayList::new);
    public static final MapCodec<LineAnnotation> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(LineAnnotation::x),
            Codec.INT.fieldOf("y").forGetter(LineAnnotation::y),
            Codec.INT.fieldOf("z").forGetter(LineAnnotation::z),
            Codec.FLOAT.fieldOf("width").forGetter(LineAnnotation::width),
            Codec.INT.fieldOf("color").forGetter(LineAnnotation::color),
            BYTE_LIST_CODEC.fieldOf("offsets").forGetter(LineAnnotation::offsets)
    ).apply(instance, LineAnnotation::new));
}
