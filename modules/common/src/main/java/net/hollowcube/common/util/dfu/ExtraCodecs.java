package net.hollowcube.common.util.dfu;

import net.hollowcube.common.util.BlockUtil;
import net.hollowcube.common.util.Either;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class ExtraCodecs {
    public static final Codec<Integer> INT_STRING = Codec.STRING.transform(Integer::parseInt, String::valueOf);
    public static final Codec<Long> LONG_STRING = Codec.STRING.transform(Long::parseLong, String::valueOf);

    public static final Codec<NamedTextColor> NAMED_TEXT_COLOR = Codec.STRING.transform(
            NamedTextColor.NAMES::valueOrThrow, NamedTextColor.NAMES::keyOrThrow);

    public static final StructCodec<Pos> POS = StructCodec.struct(
            "x", clamppedDouble(-30000000, 30000000), Pos::x,
            "y", clamppedDouble(-30000000, 30000000), Pos::y,
            "z", clamppedDouble(-30000000, 30000000), Pos::z,
            "yaw", Codec.FLOAT.optional(0f), Pos::yaw,
            "pitch", Codec.FLOAT.optional(0f), Pos::pitch,
            Pos::new);

    public static final Codec<Block> BLOCK_STATE_STRING = Codec.STRING
            .transform(BlockUtil::fromStringOld, BlockUtil::toString);
    public static final Codec<Block> BLOCK_NAME_STRING = Codec.STRING
            .transform(Block::fromKey, Block::name);

    public static <L, R> Codec<Either<L, R>> either(Codec<L> leftCodec, Codec<R> rightCodec) {
        return new Codec<>() {
            @Override
            public <D> Result<Either<L, R>> decode(Transcoder<D> coder, D value) {
                if (leftCodec.decode(coder, value) instanceof Result.Ok(L left))
                    return new Result.Ok<>(Either.left(left));
                if (rightCodec.decode(coder, value) instanceof Result.Ok(R right))
                    return new Result.Ok<>(Either.right(right));
                return new Result.Error<>("Invalid value for either codec");
            }

            @Override
            public <D> Result<D> encode(Transcoder<D> coder, @Nullable Either<L, R> value) {
                if (value == null) return new Result.Error<>("null");
                if (value.isLeft()) return leftCodec.encode(coder, value.left());
                if (value.isRight()) return rightCodec.encode(coder, value.right());
                return new Result.Error<>("Invalid value for either codec");
            }
        };
    }

    //    public static final Codec<Object> ANY_PRIMITIVE = net.kyori.adventure.util.Codec.of(new Encoder<>() {
//        @Override
//        public <T> DataResult<T> encode(Object input, DynamicOps<T> ops, T prefix) {
//            return switch (input) {
//                case Boolean b -> DataResult.success(ops.createBoolean(b));
//                case Byte b -> DataResult.success(ops.createByte(b));
//                case Short s -> DataResult.success(ops.createShort(s));
//                case Integer i -> DataResult.success(ops.createInt(i));
//                case Long l -> DataResult.success(ops.createLong(l));
//                case Float f -> DataResult.success(ops.createFloat(f));
//                case Double d -> DataResult.success(ops.createDouble(d));
//                case String s -> DataResult.success(ops.createString(s));
//                default -> DataResult.error(() -> "Unsupported/non-primitive type: " + input.getClass());
//            };
//        }
//    }, new Decoder<>() {
//        @Override
//        public <T> DataResult<Pair<Object, T>> decode(DynamicOps<T> ops, T input) {
//            var bool = ops.getBooleanValue(input);
//            if (bool.result().isPresent())
//                return DataResult.success(Pair.of(bool.result().get(), ops.empty()));
//            var number = ops.getNumberValue(input);
//            if (number.result().isPresent())
//                return DataResult.success(Pair.of(number.result().get(), ops.empty()));
//            var str = ops.getStringValue(input);
//            if (str.result().isPresent())
//                return DataResult.success(Pair.of(str.result().get(), ops.empty()));
//            return DataResult.error(() -> "Unsupported/non-primitive type: " + input);
//        }
//    });
//
//    public static final Codec<Color> COLOR = Codec.withAlternative(
//            Codec.STRING.comapFlatMap(
//                    text -> result(ColorUtil.fromHex(text), "Invalid color: " + text),
//                    ColorUtil::toHex
//            ),
//            Codec.INT.xmap(Color::new, Color::asRGB)
//    );
//
//    // Enum as ordinal integer
//    @Deprecated
//    public static <T extends Enum<T>> Codec<T> EnumI(Class<T> enumClass) {
//        var values = enumClass.getEnumConstants();
//        return Codec.INT.xmap(ord -> values[ord], Enum::ordinal);
//    }
//
//    // Enum as string
//    public static <T extends Enum<T>> Codec<T> Enum(Class<T> enumClass) {
//        var values = enumClass.getEnumConstants();
//        final Codec<T> stringCodec = Codec.STRING.comapFlatMap(
//                name -> {
//                    try {
//                        return DataResult.success(Enum.valueOf(enumClass, name.toUpperCase(Locale.ROOT)));
//                    } catch (IllegalArgumentException e) {
//                        String expected = Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
//                        return DataResult.error(() -> "Invalid enum name: " + name + " (expected one of: " + expected + ")");
//                    }
//                },
//                value -> value.name().toLowerCase(Locale.ROOT)
//        );
//        final Codec<T> intCodec = Codec.INT.comapFlatMap(
//                ord -> ord < 0 || ord > values.length ? DataResult.error(() -> "Invalid ordinal: " + ord) : DataResult.success(values[ord]),
//                Enum::ordinal
//        );
//
//        return Codec.withAlternative(stringCodec, intCodec);
//    }
//
    public static Codec<Double> clamppedDouble(double min, double max) {
        return Codec.DOUBLE.transform(
                it -> Math.min(max, Math.max(min, it)),
                it -> Math.min(max, Math.max(min, it))
        );
    }

    public static Codec<Integer> clamppedInt(int min, int max) {
        return Codec.INT.transform(
                it -> Math.min(max, Math.max(min, it)),
                it -> Math.min(max, Math.max(min, it))
        );
    }

//    public static <T> DataResult<T> result(@Nullable T value, String message) {
//        return value == null ? DataResult.error(() -> message) : DataResult.success(value);
//    }
//
//    public static <I, O> Function<I, Optional<O>> optional(Function<I, O> mapper) {
//        return input -> Optional.ofNullable(mapper.apply(input));
//    }

    public static <K, V> StructCodec<Map<K, V>> dispatchedMap(Codec<K> keyCodec, Function<K, Codec<V>> valueCodec) {
        return new StructCodec<>() {
            @Override
            public <D> Result<Map<K, V>> decodeFromMap(Transcoder<D> coder, Transcoder.MapLike<D> map) {
                if (map.isEmpty()) return new Result.Ok<>(Map.of());

                final Map<K, V> decodedMap = new HashMap<>(map.size());
                for (final String key : map.keys()) {
                    final Result<K> keyResult = keyCodec.decode(coder, coder.createString(key));
                    if (!(keyResult instanceof Result.Ok(K decodedKey)))
                        return keyResult.cast();
                    // The throwing decode here is fine since we are already iterating over known keys.
                    final Result<V> valueResult = valueCodec.apply(decodedKey).decode(coder, map.getValue(key).orElseThrow());
                    if (!(valueResult instanceof Result.Ok(V decodedValue)))
                        return valueResult.cast();
                    decodedMap.put(decodedKey, decodedValue);
                }
                return new Result.Ok<>(Map.copyOf(decodedMap));
            }

            @Override
            public <D> Result<D> encodeToMap(Transcoder<D> coder, Map<K, V> value, Transcoder.MapBuilder<D> map) {
                if (value.isEmpty()) return new Result.Ok<>(map.build());

                for (final Map.Entry<K, V> entry : value.entrySet()) {
                    final Result<D> keyResult = keyCodec.encode(coder, entry.getKey());
                    if (!(keyResult instanceof Result.Ok(D encodedKey)))
                        return keyResult.cast();
                    final Result<D> valueResult = valueCodec.apply(entry.getKey()).encode(coder, entry.getValue());
                    if (!(valueResult instanceof Result.Ok(D encodedValue)))
                        return valueResult.cast();
                    map.put(encodedKey, encodedValue);
                }

                return new Result.Ok<>(map.build());
            }
        };
    }

    /// Behaves like Minestom RegistryTaggedUnion, but does not require registries to be present in the codec.
    public static <T> StructCodec<T> ExtRegistryCodec(
            DynamicRegistry<StructCodec<? extends T>> registry,
            Function<T, StructCodec<? extends T>> serializerGetter,
            String key
    ) {
        return new StructCodec<T>() {
            @Override
            public <D> Result<T> decodeFromMap(Transcoder<D> coder, Transcoder.MapLike<D> map) {
                final Result<String> type = map.getValue(key).map(coder::getString);
                if (!(type instanceof Result.Ok(@KeyPattern String tag)))
                    return type.mapError(e -> key + ": " + e).cast();
                final StructCodec<T> innerCodec = (StructCodec<T>) registry.get(Key.key(tag));
                if (innerCodec == null) return new Result.Error<>("No such key: " + tag);

                return innerCodec.decodeFromMap(coder, map);
            }

            @Override
            public <D> Result<D> encodeToMap(Transcoder<D> coder, T value, Transcoder.MapBuilder<D> map) {
                //noinspection unchecked
                final StructCodec<T> innerCodec = (StructCodec<T>) serializerGetter.apply(value);
                final RegistryKey<StructCodec<? extends T>> type = registry.getKey(innerCodec);
                if (type == null) return new Result.Error<>("Unregistered serializer for: " + value);

                map.put(key, coder.createString(type.key().asString()));
                return innerCodec.encodeToMap(coder, value, map);
            }
        };
    }
}
