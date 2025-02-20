package net.hollowcube.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class CollectionUtil {

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
}
