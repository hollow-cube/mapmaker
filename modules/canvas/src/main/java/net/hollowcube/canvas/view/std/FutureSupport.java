package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.view.State;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.canvas.view.ViewFunc;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;

public final class FutureSupport {
    private static final System.Logger logger = System.getLogger(FutureSupport.class.getName());

    private FutureSupport() {}

    private static final State<Result<?>> resultState = State.value("result", Result::ofNull);

    public static @NotNull View Loading(@NotNull ViewContext context, @NotNull FutureResult<?> future, @NotNull ViewFunc loadingComp, @NotNull ViewFunc loadedComp, @NotNull ViewFunc errorComp) {
        var result = context.get(resultState, () -> {
            if (future.toCompletableFuture().isDone()) {
                try {
                    return future.toCompletableFuture().get();
                } catch (InterruptedException | ExecutionException e) {
                    return null; // Handled by below case
                }
            }
            return null;
        });

        // If no result it must be loading
        if (result == null) {
            //todo bring back flag, add test for outdated future
            future.alsoRaw(res -> {
                if (res.isErr())
                    logger.log(System.Logger.Level.ERROR, "Failed to load future: {}", res.error().message());
                context.set(resultState, res);
            });
            return context.create("loadingComp", loadingComp);
        }

        System.out.println("loaded");
        return context.create("loadedComp", result.isErr() ? errorComp : loadedComp);
    }

}
