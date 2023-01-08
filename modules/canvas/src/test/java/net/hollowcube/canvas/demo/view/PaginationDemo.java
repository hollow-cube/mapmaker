package net.hollowcube.canvas.demo.view;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.canvas.view.std.Pagination;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class PaginationDemo {

    public static @NotNull View PaneDemo(@NotNull ViewContext context) {
        var pane = View.Pane(9, 6);
        var ctrl = Pagination.Controller(context, 7, 4, PaginationDemo::DemoPage,
                c -> View.Button(ItemStack.of(Material.BARRIER).withDisplayName(Component.text("empty")), ClickHandler.noop()));
        pane.add(1, 1, ctrl.PageView());
        pane.add(1, 5, ctrl.NextPageButton(View.Item(3, 1, ItemStack.of(Material.ARROW).withDisplayName(Component.text("Next page")))));
        pane.add(5, 5, ctrl.PrevPageButton(View.Item(3, 1, ItemStack.of(Material.ARROW).withDisplayName(Component.text("Next page")))));
        return pane;
    }

    private static @NotNull Pagination.Page DemoPage(@NotNull ViewContext context, int page, int pageWidth, int pageHeight) {
        var item = Material.fromId(Material.WHITE_STAINED_GLASS_PANE.id() + page);
        return new Pagination.Page(View.Button(pageWidth, pageHeight, ItemStack.of(item), ClickHandler.leftClick(() -> {
            System.out.println("Clicked page " + page);
        })), page < 8);
    }
}
