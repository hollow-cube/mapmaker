package net.hollowcube.canvas.demo.view;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.State;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.NotNull;

public class CounterDemo {

    private static final State<Integer> countState = State.value("count", () -> 0);

    @Pure
    public static @NotNull View CounterDemo(@NotNull ViewContext context) {
        return CounterDemo(context, 0);
    }

    @Pure
    public static @NotNull View CounterDemo(@NotNull ViewContext context, int initial) {
        int count = context.get(countState, () -> initial);
        var pane = View.Pane(9, 1);
        pane.add(0, 0, View.Button(4, 1, ItemStack.of(Material.PAPER), ClickHandler.leftClick(() -> context.set(countState, count - 1))));
        pane.add(4, 0, View.Item(ItemStack.of(Material.DIAMOND, 1).withDisplayName(Component.text("Count: " + count))));
        pane.add(5, 0, View.Button(4, 1, ItemStack.of(Material.PAPER), ClickHandler.leftClick(() -> context.set(countState, count + 1))));
        return pane;
    }

    @Pure
    public static @NotNull View MultiCounterDemo(@NotNull ViewContext context) {
        var pane = View.Pane(9, 3);
        pane.add(0, 0, context.create("0", c -> CounterDemo(c, 1)));
        pane.add(0, 1, context.create("1", c -> CounterDemo(c, 2)));
        pane.add(0, 2, context.create("2", c -> CounterDemo(c, 3)));
        return pane;
    }

}
