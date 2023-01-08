package net.hollowcube.canvas.demo.view;

import net.hollowcube.canvas.view.State;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class FutureDemo {

    private static final State<FutureResult<?>> futureState = State.value("future", FutureResult::ofNull);

    public static @NotNull View LoadingFutureDemo(@NotNull ViewContext context) {
        var pane = View.Pane(9, 1);
        var future = context.get(futureState, () -> FutureResult.supply(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return Result.ofNull();
        }));
        pane.add(0, 0, context.create("loading", c -> View.Loading(c, future,
                View.Item(ItemStack.of(Material.PAPER).withDisplayName(Component.text("loading..."))),
                View.Item(ItemStack.of(Material.ENCHANTED_BOOK).withDisplayName(Component.text("loaded!"))),
                View.Item(ItemStack.of(Material.BARRIER).withDisplayName(Component.text("error!"))))));
        return pane;
    }

}
