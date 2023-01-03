package net.hollowcube.canvas.view;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

sealed class ViewContextImpl implements ViewContext permits ViewContextImpl.Root {

    private final Map<String, Object> map = new HashMap<>();

    private Map<String, ViewContextImpl> children = new HashMap<>();
    private Map<String, ViewContextImpl> childrenToDestroy = new HashMap<>();

    private final ViewContext parent;

    public ViewContextImpl(ViewContext parent) {
        this.parent = parent;
    }

    @Override
    public <T> @NotNull T get(@NotNull String name, @NotNull Supplier<T> def) {
        return (T) map.computeIfAbsent(name, unused -> def.get());
    }

    @Override
    public <T> @NotNull T get(@NotNull String name, @NotNull T def) {
        return (T) map.computeIfAbsent(name, unused -> def);
    }

    @Override
    public <T> void set(@NotNull String name, @NotNull T value) {
        map.put(name, value);
        redraw();
    }

    @Override
    public @NotNull View create(@NotNull String id, @NotNull ViewFunc viewFunc) {
        var existing = childrenToDestroy.remove(id);
        if (existing == null)
            existing = new ViewContextImpl(this);

        children.put(id, existing);
        return viewFunc.construct(existing);
    }

    // Implementation

    /**
     * Tells the parent to immediately redraw the {@link View}.
     * <p>
     * An optimization here would be to rerender only subtrees which had a state change. We can pass basically an
     * XPath which tells it which subtree actually changed.
     */
    public void redraw() {
        ((ViewContextImpl) Objects.requireNonNull(parent)).redraw();
    }

    public void beginRender() {
        childrenToDestroy = children;
        children = new HashMap<>();
    }

    public void endRender() {
        childrenToDestroy = null;
    }


    static final class Root extends ViewContextImpl {
        private Runnable redrawFunc;

        public Root() {
            super(null);
        }

        void setRedrawFunc(@NotNull Runnable redrawFunc) {
            this.redrawFunc = redrawFunc;
        }

        @Override
        public void redraw() {
            redrawFunc.run();
        }
    }

}
