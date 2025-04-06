package net.hollowcube.compat.axiom.data.annotations.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@SuppressWarnings("UnstableApiUsage")
public record LinesOutlineAnnotation(
        @NotNull LongList positions,
        int color
) implements AnnotationData {

    public static final NetworkBuffer.Type<LinesOutlineAnnotation> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.LONG_ARRAY.transform(LongArrayList::new, LongCollection::toLongArray), LinesOutlineAnnotation::positions,
            NetworkBuffer.INT, LinesOutlineAnnotation::color,
            LinesOutlineAnnotation::new);
    private static final Codec<LongList> LONG_LIST_CODEC = Codec.LONG.list().transform(LongArrayList::new, ArrayList::new);
    public static final StructCodec<LinesOutlineAnnotation> CODEC = StructCodec.struct(
            "positions", LONG_LIST_CODEC, LinesOutlineAnnotation::positions,
            "color", Codec.INT, LinesOutlineAnnotation::color,
            LinesOutlineAnnotation::new);
}
