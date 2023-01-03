package net.hollowcube.canvas.view;

import net.hollowcube.canvas.ItemSection;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;

public class ViewHostingSection extends ItemSection {
    private final ViewContextImpl.Root viewContext;
    private final ViewFunc viewFunc;

    private View view;

    public ViewHostingSection(@NotNull ViewFunc viewFunc) {
        this(viewFunc, new ViewContextImpl.Root());
    }

    private ViewHostingSection(@NotNull ViewFunc viewFunc, @NotNull ViewContextImpl.Root viewContext) {
        this(viewFunc, viewContext, viewFunc.construct(viewContext));
    }

    private ViewHostingSection(@NotNull ViewFunc viewFunc, @NotNull ViewContextImpl.Root context, @NotNull View initialView) {
        super(initialView.width(), initialView.height());
        this.viewContext = context;
        viewContext.setRedrawFunc(this::redraw);
        this.viewFunc = viewFunc;

        this.view = initialView;
    }

    @Override
    protected void mount() {
        super.mount();
        update();
    }

    private void update() {
        var items = view.getContents();
        for (int i = 0; i < items.length; i++) {
            setItem(i, items[i]);
        }
    }

    private void redraw() {
        viewContext.beginRender();
        view = viewFunc.construct(viewContext);
        viewContext.endRender();

        //todo assertion about width and height not changing
        update();
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        return view.handleClick(player, slot, clickType);
    }
}
