package net.hollowcube.canvas.mock;

import net.hollowcube.canvas.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MockElement implements Element {
    private final String id;

    private boolean loading = false;

    protected MockElement(@NotNull String id) {
        this.id = id;
    }

    @Override
    public @Nullable String id() {
        return id;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }
}
