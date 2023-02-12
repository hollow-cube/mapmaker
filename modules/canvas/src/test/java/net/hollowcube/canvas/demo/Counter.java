package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
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
