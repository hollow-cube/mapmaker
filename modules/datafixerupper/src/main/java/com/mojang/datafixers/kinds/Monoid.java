// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.
package com.mojang.datafixers.kinds;

import java.util.ArrayList;
import java.util.List;

public interface Monoid<T> {
    T point();

    T add(final T first, final T second);

    static <T> Monoid<List<T>> listMonoid() {
        // TODO: immutable list with structural sharing
        return new Monoid<List<T>>() {
            @Override
            public List<T> point() {
                return List.of();
            }

            @Override
            public List<T> add(final List<T> first, final List<T> second) {
                final List<T> builder = new ArrayList<>(first.size() + second.size());
                builder.addAll(first);
                builder.addAll(second);
                return List.copyOf(builder);
            }
        };
    }
}
