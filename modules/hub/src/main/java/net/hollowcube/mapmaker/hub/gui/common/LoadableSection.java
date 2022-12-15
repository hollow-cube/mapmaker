package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.mapmaker.result.Error;
import net.hollowcube.mapmaker.result.FutureResult;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a section which may need to load behind a {@link net.hollowcube.mapmaker.result.FutureResult}.
 */
public class LoadableSection<T> extends ParentSection {
    private static final int LOADING_DELAY_TICKS = 5;

    private FutureResult<T> future;

    public LoadableSection(int width, int height) {
        super(width, height);


    }

    protected void setFuture(@NotNull FutureResult<T> future) {
        Check.stateCondition(this.future != null, "future already set");
        this.future = future;

        future.then(this::renderResult).thenErr(this::renderError);
    }

    protected void renderResult(@NotNull T result) {

    }

    protected void renderError(@NotNull Error err) {

    }
}
