package net.hollowcube.compat.axiom.data.annotations.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.ArrayList;

public record LinesOutlineAnnotation(
        LongList positions,
        int color
) implements AnnotationData {

    public static final NetworkBuffer.Type<LinesOutlineAnnotation> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.LONG_ARRAY.transform(LongArrayList::new, LongCollection::toLongArray), LinesOutlineAnnotation::positions,
            NetworkBuffer.INT, LinesOutlineAnnotation::color,
            LinesOutlineAnnotation::new
    );

    private static final Codec<LongList> LONG_LIST_CODEC = Codec.LONG.listOf().xmap(LongArrayList::new, ArrayList::new);
    public static final MapCodec<LinesOutlineAnnotation> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LONG_LIST_CODEC.fieldOf("positions").forGetter(LinesOutlineAnnotation::positions),
            Codec.INT.fieldOf("color").forGetter(LinesOutlineAnnotation::color)
    ).apply(instance, LinesOutlineAnnotation::new));
}
