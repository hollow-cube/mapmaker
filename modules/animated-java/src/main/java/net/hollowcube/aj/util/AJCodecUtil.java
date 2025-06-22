package net.hollowcube.aj.util;

import net.minestom.server.codec.Codec;
import net.minestom.server.coordinate.Vec;

@SuppressWarnings("UnstableApiUsage")
public final class AJCodecUtil {
    public static final Codec<Vec> VEC = Codec.DOUBLE.list(3).transform(
            l -> new Vec(l.getFirst(), l.get(1), l.get(2)),
            v -> java.util.List.of(v.x(), v.y(), v.z())
    );
    public static final Codec<Vec> VEC_COERCED = Codec.STRING.transform(Double::parseDouble, String::valueOf).list(3)
            .transform(l -> new Vec(l.getFirst(), l.get(1), l.get(2)),
                    v -> java.util.List.of(v.x(), v.y(), v.z()));

    public static final Codec<float[]> QUATERNION = Codec.FLOAT.list(4).transform(
            l -> new float[]{l.getFirst(), l.get(1), l.get(2), l.get(3)},
            v -> java.util.List.of(v[0], v[1], v[2], v[3])
    );
    public static final Codec<float[]> FLOAT2 = Codec.FLOAT.list(2).transform(
            l -> new float[]{l.getFirst(), l.get(1)},
            v -> java.util.List.of(v[0], v[1])
    );

}
