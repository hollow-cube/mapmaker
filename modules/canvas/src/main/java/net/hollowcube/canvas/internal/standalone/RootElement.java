package net.hollowcube.canvas.internal.standalone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RootElement extends BoxElement {

    public RootElement(@Nullable String id, int width, int height, @NotNull Align align) {
        super(id, width, height, align);
    }
}
