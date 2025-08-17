package net.hollowcube.mapmaker.panels;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.thread.TickThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class Panel extends Element {
    public static final Panel EMPTY = new Panel(0, 0) {
    };

    public static @NotNull InventoryHost open(@NotNull Player player, @NotNull Panel panel) {
        final InventoryHost host = new InventoryHost(player);
        host.pushView(panel);
        return host;
    }

    record PosChild(int x, int y, Element child) {
    }

    private final InventoryType inventoryType;
    private final List<PosChild> children = new ArrayList<>();

    protected Panel(int slotWidth, int slotHeight) {
        this(InventoryType.CHEST_6_ROW, slotWidth, slotHeight);
    }

    protected Panel(@NotNull InventoryType inventoryType, int slotWidth, int slotHeight) {
        super(slotWidth, slotHeight);
        this.inventoryType = inventoryType;
    }

    public <E extends Element> @NotNull E add(int x, int y, @NotNull E element) {
        this.children.add(new PosChild(x, y, element));
        if (host != null) {
            host.queueRedraw();
            element.mount(host, true);
        }
        return element;
    }

    public void clear() {
        children.forEach(child -> child.child.unmount());
        this.children.clear();
        if (host != null) host.queueRedraw();
    }

    protected void async(@NotNull Runnable runnable) {
        final InventoryHost host = this.host;
        if (host == null) return;
        FutureUtil.submitVirtual(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                // TODO: exception should include page name for posthog.
                host.player().closeInventory();
                host.player().sendMessage(Component.translatable("generic.unknown_error"));
                ExceptionReporter.reportException(e, host.player());
            }
        });
    }

    protected void sync(@NotNull Runnable runnable) {
        final InventoryHost host = this.host;
        if (host == null) return;
        if (Thread.currentThread() instanceof TickThread) runnable.run();
        else host.player().scheduleNextTick(_ -> runnable.run());
    }

    // Impl

    @NotNull InventoryType inventoryType() {
        return this.inventoryType;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        super.build(builder);

        for (var child : children) {
            var mark = builder.mark();

            builder.boundsRect(child.x, child.y, child.child.slotWidth, child.child.slotHeight);
            child.child.build(builder);

            builder.restore(mark);
        }
    }

    @Override
    public @Nullable CompletableFuture<Void> handleClick(@NotNull ClickType clickType, int x, int y) {
        for (var child : children) {
            if (x >= child.x && x < child.x + child.child.slotWidth
                    && y >= child.y && y < child.y + child.child.slotHeight) {
                return child.child.handleClick(clickType, x - child.x, y - child.y);
            }
        }
        return null;
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        for (var child : children) {
            child.child.mount(host, isInitial);
        }
    }

    @Override
    protected void unmount() {
        super.unmount();
        for (var child : children) {
            child.child.unmount();
        }
    }

    // DSL

    protected static @NotNull Button button(@NotNull String translationKey) {
        return button(translationKey, 1, 1);
    }

    protected static @NotNull Button button(@NotNull String translationKey, int width, int height) {
        return new Button(translationKey, width, height);
    }
}
