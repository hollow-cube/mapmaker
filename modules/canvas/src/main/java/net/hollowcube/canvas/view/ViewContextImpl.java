package net.hollowcube.canvas.view;

import net.hollowcube.canvas.RouterSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

sealed class ViewContextImpl implements ViewContext permits ViewContextImpl.Root {

    private final Map<String, Object> map = new HashMap<>();
    private long flag = 0;

    private Map<String, ViewContextImpl> children = new HashMap<>();
    private Map<String, ViewContextImpl> childrenToDestroy = new HashMap<>();

    private final ViewContext parent;

    public ViewContextImpl(ViewContext parent) {
        this.parent = parent;
    }

    @Override
    public void markDirty() {
        redraw();
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
    public long flag() {
        return flag;
    }

    @Override
    public long flag(long flag) {
        this.flag = flag;
        return flag;
    }

    @Override
    public @NotNull View create(@NotNull String id, @NotNull ViewFunc viewFunc) {
        var existing = childrenToDestroy.remove(id);
        if (existing == null)
            existing = new ViewContextImpl(this);

        children.put(id, existing);
        return viewFunc.construct(existing);
    }

    @Override
    public boolean hasHistory() {
        return parent.hasHistory();
    }

    @Override
    public void pushView(int width, int height, @NotNull ViewFunc view) {
        parent.pushView(width, height, view);
    }

    @Override
    public void popView() {
        parent.popView();
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
        childrenToDestroy = new HashMap<>();
    }


    static final class Root extends ViewContextImpl {
        private RouterSection router;
        private Runnable redrawFunc;

        //todo this handles cases where redraw is called during construction. Maybe this should just be an error or something though
        private boolean needsRedraw = false;
        private boolean isDrawing = false;

        public Root() {
            super(null);
        }

        public void setRoot(@NotNull RouterSection router) {
            this.router = router;
        }

        void setRedrawFunc(@NotNull Runnable redrawFunc) {
            this.redrawFunc = redrawFunc;
            if (needsRedraw) redraw();
        }

        @Override
        public void redraw() {
            if (redrawFunc == null || isDrawing) {
                needsRedraw = true;
                return;
            }

            beginRender();
            redrawFunc.run();
            endRender();
        }

        @Override
        public void beginRender() {
            isDrawing = true;
            super.beginRender();
        }

        @Override
        public void endRender() {
            super.endRender();
            isDrawing = false;
            if (needsRedraw) {
                needsRedraw = false;
                redraw();
            }
        }

        @Override
        public boolean hasHistory() {
            return router.hasHistory();
        }

        @Override
        public void pushView(int width, int height, @NotNull ViewFunc view) {
            router.push(new ViewHostingSection(width, height, view));
        }

        @Override
        public void popView() {
            router.pop();
        }
    }

}
