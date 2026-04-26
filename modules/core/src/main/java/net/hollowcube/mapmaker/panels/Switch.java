package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

public class Switch extends Element {
    private static final int MAX_CHILDREN = 20;

    private final List<Element> children;
    private int selectedIndex = 0;
    private final BitSet mountMask = new BitSet(MAX_CHILDREN);

    private final List<IntConsumer> onSelect = new ArrayList<>();

    public Switch(int slotWidth, int slotHeight, List<Element> children) {
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
        if (selectedIndex >= 0 && selectedIndex < children.size())
            children.get(selectedIndex).unmount();
        selectedIndex = index;
        boolean isInitial = !mountMask.get(index);
        mountMask.set(index);
        children.get(index).mount(host, isInitial);

        for (var consumer : onSelect) {
            consumer.accept(index);
        }

        if (host != null) host.queueRedraw();
    }

    public void onSelect(IntConsumer consumer) {
        onSelect.add(consumer);
    }

    public Element button(int index, int width, int height, String translationKey, String sprite) {
        var button = new Button(translationKey + (index == 0 ? ".on" : ".off"), width, height);
        button.sprite(sprite + (index == 0 ? "_on" : "_off"));
        button.onLeftClick(_ -> select(index));
        onSelect.add(i -> {
            button.translationKey(translationKey + (i == index ? ".on" : ".off"));
            button.sprite(sprite + (i == index ? "_on" : "_off"));
        });
        return button;
    }

    public Element toggleButton(int width, int height, String translationKey, String sprite, int spriteX, int spriteY) {
        var button = new Button(translationKey + ".off", width, height);
        button.sprite(sprite + "_off", spriteX, spriteY);
        button.onLeftClick(_ -> select(selectedIndex == 0 ? 1 : 0));
        onSelect.add(i -> {
            button.translationKey(translationKey + (i == 1 ? ".on" : ".off"));
            button.sprite(sprite + (i == 1 ? "_on" : "_off"), spriteX, spriteY);
        });
        return button;
    }

    @Override
    public void build(MenuBuilder builder) {
        super.build(builder);
        children.get(selectedIndex).build(builder);
    }

    @Override
    public @Nullable CompletableFuture<Void> handleClick(ClickType clickType, int x, int y) {
        return children.get(selectedIndex).handleClick(clickType, x, y);
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        children.get(selectedIndex).mount(host, isInitial);
        if (isInitial) mountMask.clear();
    }

    @Override
    protected void unmount() {
        super.unmount();
        children.get(selectedIndex).unmount();
    }
}
