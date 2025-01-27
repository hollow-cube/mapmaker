package net.hollowcube.common.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Utility class for doing common operations.
 */
public class OpUtils {

    /**
     * Maps a nullable input to an output using a mapper function if the input is not null.
     */
    @Contract("null, _ -> null")
    public static <I, O> O map(@Nullable I input, Function<I, O> mapper) {
        return input == null ? null : mapper.apply(input);
    }

    /**
     * Safely casts an object to a class.
     */
    @Contract("null, _ -> null")
    public static <T> T safeCast(Object object, Class<T> clazz) {
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
