package net.hollowcube.canvas.view.std;

import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.canvas.view.ViewFunc;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import org.jetbrains.annotations.NotNull;

public final class FutureSupport {
    private static final System.Logger logger = System.getLogger(FutureSupport.class.getName());

    private FutureSupport() {}

    public static @NotNull View Loading(@NotNull ViewContext context, @NotNull FutureResult<?> future, @NotNull ViewFunc loadingComp, @NotNull ViewFunc loadedComp, @NotNull ViewFunc errorComp) {
        var result = context.<Result<?>>get("result");
        // If no result it must be loading
        if (result == null) {
            future.alsoRaw(res -> {
                if (res.isErr())
                    logger.log(System.Logger.Level.ERROR, "Failed to load future: {}", res.error().message());
                context.set("result", res);
            });
            System.out.println("loading");
            return context.create("loadingComp", loadingComp);
        }

        System.out.println("loaded");
        return context.create("loadedComp", result.isErr() ? errorComp : loadedComp);
    }

}
