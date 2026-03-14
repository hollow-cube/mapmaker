package net.hollowcube.common.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for doing common operations.
 */
public class OpUtils {
    private OpUtils() {
    }

    /**
     * Maps a nullable input to an output using a mapper function if the input is not null.
     */
    @Contract("null, _ -> null")
    public static <I, O> @Nullable O map(@Nullable I input, Function<I, @Nullable O> mapper) {
        return input == null ? null : mapper.apply(input);
    }

    /**
     * Maps a nullable input to an output using a mapper function if the input is not null, otherwise returns a fallback value.
     */
    public static <I, O> O mapOr(@Nullable I input, Function<I, O> mapper, Supplier<O> fallback) {
        if (input == null) return fallback.get();
        return Objects.requireNonNullElseGet(mapper.apply(input), fallback);
    }

    /**
     * Maps a nullable input to an output using a mapper function if the input is not null, otherwise returns a fallback value.
     */
    public static <I, O> O mapOr(@Nullable I input, Function<I, O> mapper, O fallback) {
        if (input == null) return fallback;
        return Objects.requireNonNullElse(mapper.apply(input), fallback);
    }

    /**
     * Takes an input and returns a fallback value if the input is null, the fallback can also be null.
     */
    @UnknownNullability
    public static <O, A extends O, B extends O> O or(@Nullable A input, Supplier<@UnknownNullability B> fallback) {
        return input == null ? fallback.get() : input;
    }

    /**
     * Takes an amount of nullable inputs and returns the first non-null input, or null if all inputs throws null pointer exception.
     */
    @SafeVarargs
    public static <O, A extends O> O firstNonNull(@Nullable A ...inputs) {
        for (A input : inputs) {
            if (input != null) return input;
        }
        throw new NullPointerException("All inputs are null");
    }

    /**
     * Safely casts an object to a class.
     */
    @Contract("null, _ -> null")
    @UnknownNullability
    public static <T> T safeCast(@Nullable Object object, Class<T> clazz) {
        return clazz.isInstance(object) ? clazz.cast(object) : null;
    }

    /**
     * Copies a map and applies a consumer to the copy.
     */
    public static <K, V> Map<K, V> copyAndEdit(Map<K, V> map, Consumer<Map<K, V>> consumer) {
        return build(new HashMap<>(map), consumer);
    }

    /**
     * Builds an object by applying a consumer to it.
     */
    public static <T> T build(T instance, Consumer<T> consumer) {
        consumer.accept(instance);
        return instance;
    }
}
