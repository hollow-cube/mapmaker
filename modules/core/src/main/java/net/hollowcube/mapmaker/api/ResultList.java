package net.hollowcube.mapmaker.api;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RuntimeGson
public record ResultList<T>(List<T> results) implements Iterable<T> {

    public boolean isEmpty() {
        return results.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return results.iterator();
    }

    public @Nullable T first() {
        return results.isEmpty() ? null : results.getFirst();
    }

    public <K> Map<K, T> keyBy(Function<T, K> keyMapper) {
        return results.stream().collect(toMap(keyMapper, Function.identity()));
    }
}
