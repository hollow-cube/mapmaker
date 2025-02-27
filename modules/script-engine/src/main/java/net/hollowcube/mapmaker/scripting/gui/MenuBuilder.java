package net.hollowcube.mapmaker.scripting.gui;

import it.unimi.dsi.fastutil.ints.IntIntPair;
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
import java.util.function.Consumer;

public class MenuBuilder {
    private int absWidth, absHeight;
    private int slotX, slotY, slotWidth, slotHeight;
    private final FontUIBuilder title = new FontUIBuilder();
    private final ItemStack[] items;

    private final List<IntIntPair> slotBounds = new ArrayList<>();

    public MenuBuilder(int slotWidth, int slotHeight) {
        this.slotX = this.slotY = 0;
        this.slotWidth = this.absWidth = slotWidth;
        this.slotHeight = this.absHeight = slotHeight;
        this.items = new ItemStack[slotWidth * slotHeight];
        Arrays.fill(this.items, ItemStack.builder(Material.STICK)
                .set(ItemComponent.ITEM_MODEL, "minecraft:air")
                .set(ItemComponent.HIDE_TOOLTIP)
                .build());
    }

    public void pushItemModifier(@NotNull Consumer<ItemStack.Builder> func) {

    }

    public void popItemModifier() {

    }

    public int pushSlotBounds(int x, int y) {
        if (x == 0 && y == 0) {
            return this.slotBounds.size();
        }
        this.slotBounds.add(IntIntPair.of(this.slotX, this.slotY));
        this.slotX += x;
        this.slotY += y;
        //todo bounds check
        return this.slotBounds.size() - 1;
    }

    public void restoreSlotBounds(int index) {
        if (index < 0 || index >= this.slotBounds.size()) {
            throw new IllegalArgumentException("Invalid slot bounds index");
        }

        IntIntPair pair = this.slotBounds.get(index);
        this.slotX = pair.leftInt();
        this.slotY = pair.rightInt();

        //todo This just grows slot bounds indefinitely, but that's fine for now
    }

    public <T> void editSlotsWithout(int x, int y, int width, int height, @NotNull DataComponent<T> component) {
//        final ItemStack item = baseItem.build();

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
        int startX = -1 + (this.slotX * 18) + x; // -1 accounts for the gui title offset
        int startY = 4 + (this.slotY * 18) + y; // +4 accounts for the gui title offset

        title.pushColor(FontUtil.computeVerticalOffset(startY));
        title.pos(startX);
        title.drawInPlace(sprite);
        title.popColor();
    }

    public void drawText(int x, int y, @NotNull String text) {
        int startX = -1 + (this.slotX * 18) + x; // -1 accounts for the gui title offset
        int startY = 4 + (this.slotY * 18) + y; // +4 accounts for the gui title offset

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
}
