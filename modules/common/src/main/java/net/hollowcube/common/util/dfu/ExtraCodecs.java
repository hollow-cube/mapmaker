package net.hollowcube.common.util.dfu;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.BlockUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.particle.Particle;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class ExtraCodecs {

    public static final Codec<Integer> INT_STRING = Codec.STRING.xmap(Integer::parseInt, String::valueOf);
    public static final Codec<Long> LONG_STRING = Codec.STRING.xmap(Long::parseLong, String::valueOf);

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

    public static final Codec<Material> MATERIAL = Codec.STRING
            .xmap(Material::fromNamespaceId, Material::name);

    public static final Codec<Particle> PARTICLE = Codec.STRING
            .xmap(Particle::fromNamespaceId, Particle::name);

    public static final Codec<ItemStack> ITEM_STACK = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            CompoundBinaryTag tag = (CompoundBinaryTag) ops.convertTo(NbtOps.INSTANCE, input);
            return DataResult.success(Pair.of(ItemStack.fromItemNBT(tag), ops.empty()));
        }

        @Override
        public <T> DataResult<T> encode(ItemStack input, DynamicOps<T> ops, T prefix) {
            return DataResult.success(NbtOps.INSTANCE.convertTo(ops, input.toItemNBT()));
        }
    };

    public static final Codec<Block> BLOCK_STATE_STRING = Codec.STRING
            .xmap(BlockUtil::fromString, BlockUtil::toString);

    public static final Codec<Object> ANY_PRIMITIVE = Codec.of(new Encoder<>() {
        @Override
        public <T> DataResult<T> encode(Object input, DynamicOps<T> ops, T prefix) {
            return switch (input) {
                case Boolean b -> DataResult.success(ops.createBoolean(b));
                case Byte b -> DataResult.success(ops.createByte(b));
                case Short s -> DataResult.success(ops.createShort(s));
                case Integer i -> DataResult.success(ops.createInt(i));
                case Long l -> DataResult.success(ops.createLong(l));
                case Float f -> DataResult.success(ops.createFloat(f));
                case Double d -> DataResult.success(ops.createDouble(d));
                case String s -> DataResult.success(ops.createString(s));
                default -> DataResult.error("Unsupported/non-primitive type: " + input.getClass());
            };
        }
    }, new Decoder<>() {
        @Override
        public <T> DataResult<Pair<Object, T>> decode(DynamicOps<T> ops, T input) {
            var bool = ops.getBooleanValue(input);
            if (bool.result().isPresent())
                return DataResult.success(Pair.of(bool.result().get(), ops.empty()));
            var number = ops.getNumberValue(input);
            if (number.result().isPresent())
                return DataResult.success(Pair.of(number.result().get(), ops.empty()));
            var str = ops.getStringValue(input);
            if (str.result().isPresent())
                return DataResult.success(Pair.of(str.result().get(), ops.empty()));
            return DataResult.error("Unsupported/non-primitive type: " + input);
        }
    });

    // Enum as ordinal integer
    public static <T extends Enum<T>> @NotNull Codec<T> EnumI(@NotNull Class<T> enumClass) {
        var values = enumClass.getEnumConstants();
        return Codec.INT.xmap(ord -> values[ord], Enum::ordinal);
    }

    // Enum as string
    public static <T extends Enum<T>> @NotNull Codec<T> Enum(@NotNull Class<T> enumClass) {
        return Codec.STRING.xmap(
                name -> Enum.valueOf(enumClass, name.toUpperCase(Locale.ROOT)),
                value -> value.name().toLowerCase(Locale.ROOT));
    }

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

    /**
     * <p>Attempts to decode the input with the primary codec, and if that fails tries the secondary codec.</p>
     *
     * <p>Always encoded using the primary codec.</p>
     *
     * @param primary   The first codec to try, always used for encoding
     * @param secondary The second codec to try if the first fails
     * @param <T>       The type of the codec
     * @return A codec that tries the primary codec first, then the secondary
     */
    public static <T> @NotNull Codec<T> withAlternative(@NotNull Codec<T> primary, @NotNull Codec<T> secondary) {
        return Codec.either(primary, secondary).xmap(either -> either.map(t -> t, t -> t), Either::left);
    }
}
