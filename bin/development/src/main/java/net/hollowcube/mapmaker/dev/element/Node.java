package net.hollowcube.mapmaker.dev.element;

import net.hollowcube.mapmaker.dev.render.RenderContext;
import org.jetbrains.annotations.NotNull;

public interface Node {

    void render(@NotNull RenderContext context);

}
