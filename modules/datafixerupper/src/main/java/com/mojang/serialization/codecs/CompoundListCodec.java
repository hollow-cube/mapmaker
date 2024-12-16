// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package com.mojang.serialization.codecs;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class CompoundListCodec<K, V> implements Codec<List<Pair<K, V>>> {
    private final Codec<K> keyCodec;
    private final Codec<V> elementCodec;

    public CompoundListCodec(final Codec<K> keyCodec, final Codec<V> elementCodec) {
        this.keyCodec = keyCodec;
        this.elementCodec = elementCodec;
    }

    @Override
    public <T> DataResult<Pair<List<Pair<K, V>>, T>> decode(final DynamicOps<T> ops, final T input) {
        return ops.getMapEntries(input).flatMap(map -> {
            final List<Pair<K, V>> read = new ArrayList<>();
            final Map<T, T> failed = new HashMap<>();

            final AtomicReference<DataResult<Unit>> result = new AtomicReference<>(DataResult.success(Unit.INSTANCE, Lifecycle.experimental()));

            map.accept((key, value) -> {
                final DataResult<K> k = keyCodec.parse(ops, key);
                final DataResult<V> v = elementCodec.parse(ops, value);

                final DataResult<Pair<K, V>> readEntry = k.apply2stable(Pair::new, v);

                readEntry.error().ifPresent(e -> failed.put(key, value));

                result.setPlain(result.getPlain().apply2stable((u, e) -> {
                    read.add(e);
                    return u;
                }, readEntry));
            });

            final List<Pair<K, V>> elements = List.copyOf(read);
            final T errors = ops.createMap(Map.copyOf(failed));

            final Pair<List<Pair<K, V>>, T> pair = Pair.of(elements, errors);

            return result.getPlain().map(unit -> pair).setPartial(pair);
        });
    }

    @Override
    public <T> DataResult<T> encode(final List<Pair<K, V>> input, final DynamicOps<T> ops, final T prefix) {
        final RecordBuilder<T> builder = ops.mapBuilder();

        for (final Pair<K, V> pair : input) {
            builder.add(keyCodec.encodeStart(ops, pair.getFirst()), elementCodec.encodeStart(ops, pair.getSecond()));
        }

        return builder.build(prefix);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CompoundListCodec<?, ?> that = (CompoundListCodec<?, ?>) o;
        return Objects.equals(keyCodec, that.keyCodec) && Objects.equals(elementCodec, that.elementCodec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCodec, elementCodec);
    }

    @Override
    public String toString() {
        return "CompoundListCodec[" + keyCodec + " -> " + elementCodec + ']';
    }
}
