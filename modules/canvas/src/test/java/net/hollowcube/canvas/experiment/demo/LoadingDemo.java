package net.hollowcube.canvas.experiment.demo;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;

import java.util.concurrent.ForkJoinPool;

public class LoadingDemo extends View {

    private @Outlet("label") Label label;

    public LoadingDemo() {
        label.setLoading(true);
        ForkJoinPool.commonPool().submit(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            label.setLoading(false);
        });
    }

}
