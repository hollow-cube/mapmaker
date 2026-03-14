package net.hollowcube.common.util;

import net.minestom.server.codec.Codec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class CollectionUtil {
    private CollectionUtil() {
    }

    @SafeVarargs
    public static <T> List<T> copyWithMinSize(int minSize, Supplier<T> fallback, T... elements) {
        if (elements.length > minSize) return List.of(elements);
        List<T> list = new ArrayList<>(minSize);
        list.addAll(Arrays.asList(elements));
        for (int i = elements.length; i < minSize; i++) {
            list.add(fallback.get());
        }
        return List.copyOf(list);
    }

    public static <T> Codec<List<T>> minSizeList(Codec<T> codec, int minSize, Supplier<T> fallback) {
        return codec.list().transform(
                list -> {
                    if (list.size() >= minSize) return list;
                    var newList = new ArrayList<T>(minSize);
                    newList.addAll(list);
                    for (int i = list.size(); i < minSize; i++) {
                        newList.add(fallback.get());
                    }
                    return List.copyOf(newList);
                },
                list -> list
        );
    }

}
