package net.hollowcube.canvas.experiment.demo;

import net.hollowcube.canvas.experiment.Label;
import net.hollowcube.canvas.experiment.View;
import net.hollowcube.canvas.experiment.annotation.Action;
import net.hollowcube.canvas.experiment.annotation.Outlet;
import net.kyori.adventure.text.Component;

public class Counter extends View {

    private int count = 0;

    private @Outlet("count") Label label;

    public Counter() {
        label.setArgs(Component.text(count));
    }

    @Action("incr")
    private void increment() {
        label.setArgs(Component.text(++count));
    }

    @Action("decr")
    private void decrement() {
        label.setArgs(Component.text(--count));
    }

}
