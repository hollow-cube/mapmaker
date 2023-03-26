package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FutureUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class LoadingDemo extends View {

    private @Outlet("label") Label label;

    public LoadingDemo(@NotNull Context context) {
        super(context);

        label.setLoading(true);
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            label.setLoading(false);
        }).exceptionally(FutureUtil::handleException);
    }

}
