package net.hollowcube.canvas.experiment;

import org.jetbrains.annotations.NotNull;

import java.util.stream.Collector;

public abstract class View {

    static @NotNull Collector<View, ?, View> autoLayout(int width, int height) {
        throw new UnsupportedOperationException("not implemented");
    }

    protected void mount() {}

}
