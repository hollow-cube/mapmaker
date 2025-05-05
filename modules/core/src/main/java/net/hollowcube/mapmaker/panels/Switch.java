package net.hollowcube.mapmaker.panels;

import net.hollowcube.canvas.ClickType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

public class Switch extends Element {
    private final List<Element> children;
    private int selectedIndex = 0;

    private final List<IntConsumer> onSelect = new ArrayList<>();

    public Switch(int slotWidth, int slotHeight, @NotNull List<Element> children) {
        super(slotWidth, slotHeight);
        this.children = children;

        if (children.isEmpty()) {
            throw new IllegalArgumentException("Switch must have at least one child element");
        }
    }

    public void select(int index) {
        if (index == selectedIndex) return;
        if (index < 0 || index >= children.size()) {
            throw new IndexOutOfBoundsException("Index out of bounds: " + index);
        }
        selectedIndex = index;
        for (var consumer : onSelect) {
            consumer.accept(index);
        }
        if (host != null) host.queueRedraw();
    }

    public void onSelect(IntConsumer consumer) {
        onSelect.add(consumer);
    }

    public @NotNull Element button(int index, int width, int height, @NotNull String translationKey, @NotNull String sprite) {
        var button = new Button(translationKey + (index == 0 ? ".on" : ".off"), width, height);
        button.sprite(sprite + (index == 0 ? "_on" : "_off"));
        button.onLeftClick(_ -> select(index));
        onSelect.add(i -> {
            button.translationKey(translationKey + (i == index ? ".on" : ".off"));
            button.sprite(sprite + (i == index ? "_on" : "_off"));
        });
        return button;
    }

    @Override
    public void build(@NotNull MenuBuilder builder) {
        super.build(builder);
        children.get(selectedIndex).build(builder);
    }

    @Override
    public @Nullable CompletableFuture<Void> handleClick(@NotNull Player player, @NotNull ClickType clickType, int x, int y) {
        return children.get(selectedIndex).handleClick(player, clickType, x, y);
    }

    @Override
    protected void mount(@NotNull InventoryHost host) {
        super.mount(host);
        children.forEach(child -> child.mount(host));
    }
}
