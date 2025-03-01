package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponent;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuBuilder {
    private final int absWidth, absHeight, containerSlotHeight;
    private int slotX, slotY, slotWidth, slotHeight;
    private final FontUIBuilder title = new FontUIBuilder();
    private final ItemStack[] items;

    private final List<Bounds> slotBounds = new ArrayList<>();

    public MenuBuilder(int slotWidth, int slotHeight, int containerSlotHeight) {
        this.slotX = this.slotY = 0;
        this.slotWidth = this.absWidth = slotWidth;
        this.slotHeight = this.absHeight = slotHeight;
        this.containerSlotHeight = containerSlotHeight;
        this.items = new ItemStack[slotWidth * slotHeight];
        Arrays.fill(this.items, ItemStack.builder(Material.STICK)
                .set(ItemComponent.ITEM_MODEL, "minecraft:air")
                .set(ItemComponent.HIDE_TOOLTIP)
                .build());
    }

    public int availWidth() {
        return this.slotWidth - this.slotX;
    }

    public int availHeight() {
        return this.slotHeight - this.slotY;
    }

    public record Bounds(int x, int y, int width, int height) {
    }

    public int mark() {
        return this.slotBounds.size();
    }

    public void restore(int bounds) {
        Bounds last = this.slotBounds.get(bounds);
        while (this.slotBounds.size() > bounds) {
            this.slotBounds.removeLast();
        }

        this.slotX = last.x();
        this.slotY = last.y();
        this.slotWidth = last.width();
        this.slotHeight = last.height();
    }

    public void boundsRect(int x, int y) {
        this.slotBounds.add(new Bounds(this.slotX, this.slotY, this.slotWidth, this.slotHeight));
        this.slotX += x;
        this.slotY += y;
    }

    public void boundsRect(int x, int y, int width, int height) {
        this.slotBounds.add(new Bounds(this.slotX, this.slotY, this.slotWidth, this.slotHeight));
        this.slotX += x;
        this.slotY += y;
        this.slotWidth = this.slotX + width;
        this.slotHeight = this.slotY + height;
    }

    public <T> void editSlotsWithout(int x, int y, int width, int height, @NotNull DataComponent<T> component) {
        int startX = this.slotX + x;
        int startY = this.slotY + y;
        int endX = startX + width;
        int endY = startY + height;
        if (startX < 0 || startY < 0 || endX > this.absWidth || endY > this.absHeight) {
            throw new IllegalArgumentException("Out of bounds " + startX + " " + startY + " " + endX + " " + endY);
        }

        for (int i = startY; i < endY; i++) {
            for (int j = startX; j < endX; j++) {
                this.items[i * this.absWidth + j] = this.items[i * this.absWidth + j].without(component);
            }
        }
    }

    public <T> void editSlots(int x, int y, int width, int height, @NotNull DataComponent<T> component, @NotNull T data) {
//        final ItemStack item = baseItem.build();

        int startX = this.slotX + x;
        int startY = this.slotY + y;
        int endX = startX + width;
        int endY = startY + height;
        if (startX < 0 || startY < 0 || endX > this.absWidth || endY > this.absHeight) {
            throw new IllegalArgumentException("Out of bounds");
        }

        for (int i = startY; i < endY; i++) {
            for (int j = startX; j < endX; j++) {
                this.items[i * this.absWidth + j] = this.items[i * this.absWidth + j].with(component, data);
            }
        }
    }

    public void draw(int x, int y, @NotNull BadSprite sprite) {
        int startX = computeAbsoluteX(x), startY = computeAbsoluteY(y);

        title.pushColor(FontUtil.computeVerticalOffset(startY));
        title.pos(startX);
        title.drawInPlace(sprite);
        title.popColor();
    }

    public void drawText(int x, int y, @NotNull String text) {
        int startX = computeAbsoluteX(x), startY = computeAbsoluteY(y);

        // Account for font height. Not sure this is the solution i want for that.
        startY += FontUtil.DEFAULT_HEIGHT - 1;

        title.pushColor(FontUtil.computeVerticalOffset(startY));
        title.pos(startX);
        title.append(text);
        title.popColor();
    }

    public Component getTitle() {
        return this.title.build();
    }

    public ItemStack[] getItems() {
        return items;
    }

    private int computeAbsoluteX(int offset) {
        // -1 accounts for the gui title offset
        return -1 + (this.slotX * 18) + offset;
    }

    private int computeAbsoluteY(int offset) {
        // +4 accounts for the gui title offset
        int y = 4 + (this.slotY * 18) + offset;
        // If we are past the game container slots into player inv we have to account for that gap:
        if (this.slotY >= this.containerSlotHeight) y += 14;
        // If we are past the player inv we need to account for the player inv -> hotbar gap:
        if (this.slotY >= this.containerSlotHeight + 3) y += 4;
        return y;
    }
}
