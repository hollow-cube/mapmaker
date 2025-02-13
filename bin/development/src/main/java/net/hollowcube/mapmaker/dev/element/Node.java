package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.dev.element.PropHelper.getEnum;

public abstract class Node {
    private Layer layer = Layer.DEFAULT;

    public @NotNull Layer layer() {
        return this.layer;
    }

    public @NotNull Node readProps(@NotNull Value props, @NotNull Value[] children) {
        this.layer = getEnum(props, "layer", Layer.DEFAULT);
        return this;
    }

    public abstract void render(@NotNull RenderContext context);

}
