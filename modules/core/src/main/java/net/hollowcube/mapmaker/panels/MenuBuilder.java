package net.hollowcube.mapmaker.panels;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponent;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.TooltipDisplay;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

public class MenuBuilder {
    @TestOnly
    public static final ItemStack EMPTY_ITEM = ItemStack.builder(Material.STICK)
            .set(DataComponents.ITEM_MODEL, "minecraft:air")
            .set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, Set.of()))
            // Need to remove the name because the item can appear on the hotbar.
            .set(DataComponents.CUSTOM_NAME, Component.empty())
            .build();

    private final int absWidth, absHeight, containerSlotHeight;
    private final IntIntPair[] slotPosOverrides;

    private int slotX, slotY, slotWidth, slotHeight;
    private final FontUIBuilder title = new FontUIBuilder();
    private final ItemStack[] items;

    public MenuBuilder(int slotWidth, int slotHeight, int containerSlotHeight, IntIntPair[] slotPosOverrides) {
        this.slotPosOverrides = slotPosOverrides;

        this.slotX = this.slotY = 0;
        this.slotWidth = this.absWidth = slotWidth;
        this.slotHeight = this.absHeight = slotHeight;
        this.containerSlotHeight = containerSlotHeight;
        this.items = new ItemStack[slotWidth * slotHeight];
        Arrays.fill(this.items, EMPTY_ITEM);
    }

    public int availWidth() {
        return this.slotWidth - this.slotX;
    }

    public int availHeight() {
        return this.slotHeight - this.slotY;
    }

    public int absoluteX() {
        return this.slotX;
    }

    public int absoluteY() {
        return this.slotY;
    }

    public record Bounds(int x, int y, int width, int height) {

        public Bounds {
            Check.argCondition(x > 255, "x must be less than 255");
            Check.argCondition(y > 255, "x must be less than 255");
            Check.argCondition(width > 255, "x must be less than 255");
            Check.argCondition(height > 255, "x must be less than 255");
        }

        public Bounds(int value) {
            this(value & 0xFF, (value >> 8) & 0xFF, (value >> 16) & 0xFF, (value >> 24) & 0xFF);
        }

        public int value() {
            return this.x | (this.y << 8) | (this.width << 16) | (this.height << 24);
        }
    }

    public int mark() {
        return new Bounds(slotX, slotY, slotWidth, slotHeight).value();
    }

    public void restore(int bounds) {
        var last = new Bounds(bounds);
        this.slotX = last.x();
        this.slotY = last.y();
        this.slotWidth = last.width();
        this.slotHeight = last.height();
    }

    public void boundsRect(int x, int y) {
        this.slotX += x;
        this.slotY += y;
    }

    public void boundsRect(int x, int y, int width, int height) {
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
        editSlots(x, y, width, height, component, (Function<T, T>) _ -> data);
    }

    public <T> void editSlots(int x, int y, int width, int height, @NotNull DataComponent<T> component, @NotNull Function<T, T> editor) {
        int startX = this.slotX + x;
        int startY = this.slotY + y;
        int endX = startX + width;
        int endY = startY + height;
        if (startX < 0 || startY < 0 || endX > this.absWidth || endY > this.absHeight) {
            throw new IllegalArgumentException("Out of bounds " + startX + " " + startY + " " + endX + " " + endY);
        }

        for (int i = startY; i < endY; i++) {
            for (int j = startX; j < endX; j++) {
                var item = this.items[i * this.absWidth + j];
                this.items[i * this.absWidth + j] = item.with(component, editor.apply(item.get(component)));
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

    public void drawText(int x, int y, @NotNull String text, int width) {
        int startX = computeAbsoluteX(x), startY = computeAbsoluteY(y);

        // Account for font height. Not sure this is the solution i want for that.
        startY += FontUtil.DEFAULT_HEIGHT - 1;

        title.pushShadowColor(FontUtil.computeVerticalOffsetShadow(startY));
        title.pushColor(FontUtil.computeVerticalOffset(startY));
        title.pos(startX);
        title.append(text, width);
        title.popColor();
        title.popShadowColor();
    }

    public Component getTitle() {
        return this.title.build();
    }

    public ItemStack[] getItems() {
        return items;
    }

    private int computeAbsoluteX(int offset) {
        var override = getPositionOverride();
        if (override != null) {
            return override.firstInt() + offset;
        }
        // -1 accounts for the gui title offset
        return -1 + (this.slotX * 18) + offset;
    }

    private int computeAbsoluteY(int offset) {
        var override = getPositionOverride();
        if (override != null) {
            return override.secondInt() + offset;
        }

        // +4 accounts for the gui title offset
        int y = 4 + (this.slotY * 18) + offset;
        // If we are past the game container slots into player inv we have to account for that gap:
        if (this.slotY >= this.containerSlotHeight) y += 13;
        // If we are past the player inv we need to account for the player inv -> hotbar gap:
        if (this.slotY >= this.containerSlotHeight + 3) y += 4;
        return y;
    }

    private IntIntPair getPositionOverride() {
        int index = this.slotY * this.absWidth + this.slotX;
        if (index < 0 || index >= this.slotPosOverrides.length) return null;
        return this.slotPosOverrides[index];
    }
}
