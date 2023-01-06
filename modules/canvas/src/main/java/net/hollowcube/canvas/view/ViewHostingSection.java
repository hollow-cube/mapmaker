package net.hollowcube.canvas.view;

import net.hollowcube.canvas.ItemSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.canvas.RouterSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;

public class ViewHostingSection extends ItemSection {
    private static final System.Logger logger = System.getLogger(ViewHostingSection.class.getName());

    private final ViewFunc viewFunc;
    private ViewContextImpl.Root viewContext;

    private View view = null;

    public ViewHostingSection(int width, int height, @NotNull ViewFunc viewFunc) {
        super(width, height);

        this.viewFunc = viewFunc;
    }

    @Override
    protected void mount() {
        super.mount();
        if (viewContext == null) {
            viewContext = new ViewContextImpl.Root(getContext(Player.class));
            viewContext.setRedrawFunc(this::redraw);
        }

        find(RootSection.class).setTitle(Component.text(""));

        viewContext.setRoot(find(RouterSection.class));
        viewContext.redraw();
    }

    private void update() {
        var items = view.getContents();
        for (int i = 0; i < items.length; i++) {
            setItem(i, items[i]);
        }
    }

    private void redraw() {
        //todo maybe should throttle this to once per tick

//        MinecraftServer.getSchedulerManager().buildTask(() -> {
        view = viewFunc.construct(viewContext);

        //todo assertion about width and height not changing
        update();
//        }).executionType(ExecutionType.SYNC).schedule();
    }

    @Override
    protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
        return view.handleClick(player, slot, clickType);
    }
}
