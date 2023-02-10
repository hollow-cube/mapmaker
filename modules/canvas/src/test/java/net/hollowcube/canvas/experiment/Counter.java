package net.hollowcube.canvas.experiment;

import net.hollowcube.canvas.experiment.annotation.Action;
import net.hollowcube.canvas.experiment.annotation.Outlet;
import net.kyori.adventure.text.Component;

public class Counter extends View {

    private int count = 0;

    private @Outlet Label label;

    @Action
    private void increment() {
        label.setArgs(Component.text(++count));
    }

    @Action
    private void decrement() {
        label.setArgs(Component.text(--count));
    }

}
