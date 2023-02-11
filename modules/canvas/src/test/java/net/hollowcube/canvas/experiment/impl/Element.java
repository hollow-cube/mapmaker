package net.hollowcube.canvas.experiment.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Element {

    @Nullable String id();

    int width();

    int height();

    default @Nullable Element findById(@NotNull String id) {
        if (id.equals(id())) return this;
        return null;
    }

}
