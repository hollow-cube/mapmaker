package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.view.MockView;
import net.hollowcube.canvas.view.MockViewContext;
import net.hollowcube.common.result.FutureResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

/** {@link net.hollowcube.canvas.view.std.FutureSupport#Loading(net.hollowcube.canvas.view.ViewContext, net.hollowcube.common.result.FutureResult, net.hollowcube.canvas.view.ViewFunc, net.hollowcube.canvas.view.ViewFunc, net.hollowcube.canvas.view.ViewFunc)} */
public class LoadingViewTest {

    @Test
    public void testLoadingState() {
        var context = new MockViewContext();
        var future = new CompletableFuture<>();

        MockView loadingView = new MockView(), loadedView = new MockView(), errorView = new MockView();
        context.safeRender(c -> FutureSupport.Loading(c, FutureResult.wrap(future), loadingView, loadedView, errorView));

        // Only loading should have been rendered
        loadingView.assertRendered(1);
        context.assertChildPresent("loadingComp");
        loadedView.assertRendered(0);
        errorView.assertRendered(0);
    }

    @Test
    public void testLoadedState() {
        var context = new MockViewContext();
        var future = CompletableFuture.completedFuture(null);

        MockView loadingView = new MockView(), loadedView = new MockView(), errorView = new MockView();
        context.safeRender(c -> FutureSupport.Loading(c, FutureResult.wrap(future), loadingView, loadedView, errorView));

        // Only loading should have been rendered
        loadingView.assertRendered(0);
        loadedView.assertRendered(1);
        context.assertChildPresent("loadedComp");
        errorView.assertRendered(0);
    }

    @Test
    public void testErrorState() {
        var context = new MockViewContext();
        var future = CompletableFuture.failedFuture(new RuntimeException());

        MockView loadingView = new MockView(), loadedView = new MockView(), errorView = new MockView();
        context.safeRender(c -> FutureSupport.Loading(c, FutureResult.wrap(future), loadingView, loadedView, errorView));

        // Only loading should have been rendered
        loadingView.assertRendered(0);
        loadedView.assertRendered(0);
        errorView.assertRendered(1);
        context.assertChildPresent("loadedComp");
    }

    @Test
    public void testLoadingStateIntoLoaded() {
        var context = new MockViewContext();
        var future = new CompletableFuture<>();

        MockView loadingView = new MockView(), loadedView = new MockView(), errorView = new MockView();
        context.safeRender(c -> FutureSupport.Loading(c, FutureResult.wrap(future), loadingView, loadedView, errorView));

        // When complete, the view should attempt to redraw (switch to loaded state)
        future.complete(null);
        context.assertRedrawCount(1);

        // If we render again then it should call the loaded state
        context.safeRender(c -> FutureSupport.Loading(c, FutureResult.wrap(future), loadingView, loadedView, errorView));
        loadingView.assertRendered(1);
        loadedView.assertRendered(1);
        errorView.assertRendered(0);

        // Loading comp should be gone, but loaded should now be present
        context.assertChildNotPresent("loadingComp");
        context.assertChildPresent("loadedComp");
    }

    @Test
    public void testFutureChange() {
        //todo add this test. If the future changes old ones should not affect anything.
    }

}
