package net.hollowcube.mapmaker.util.dfu;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public final class ExtraCodecs {

    public static final Codec<PotionEffect> POTION_EFFECT = Codec.STRING.xmap(PotionEffect::fromNamespaceId, PotionEffect::name);

    public static final Codec<Point> POINT = Codec.DOUBLE.listOf().xmap(list -> {
        Check.stateCondition(list.size() != 3, "Expected 3 doubles, got " + list.size());
        return new Vec(list.get(0), list.get(1), list.get(2));
    }, point -> List.of(point.x(), point.y(), point.z()));

    public static final Codec<Pos> POS = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("x").forGetter(Pos::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Pos::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Pos::z),
            Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Pos::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Pos::pitch)
    ).apply(i, Pos::new));

    public static <T> @NotNull Codec<T> Lazy(Supplier<Codec<T>> supplier) {
        return new Codec<T>() {
            private Codec<T> codec = null;

            @Override
            public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
                if (codec == null) codec = supplier.get();
                return codec.decode(ops, input);
            }

            @Override
            public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
                if (codec == null) codec = supplier.get();
                return codec.encode(input, ops, prefix);
            }
        };
    }
}
