package net.hollowcube.mapmaker.dev.render;

import it.unimi.dsi.fastutil.ints.IntIntPair;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RenderContext {
    private int slotX = 0;
    private int slotY = 0;
    private final int slotWidth;
    private final int slotHeight;

    private final List<IntIntPair> offsets = new ArrayList<>();
    private final FontUIBuilder title = new FontUIBuilder();

    public RenderContext(int slotWidth, int slotHeight) {
        this.slotWidth = slotWidth;
        this.slotHeight = slotHeight;

//        title.unsafeOffset(-1); // Start at -1 on x axis to account for title position starting at 0
    }

    public int slotWidth() {
        return slotWidth;
    }

    public int slotHeight() {
        return slotHeight;
    }

    public void pushRect(int slotX, int slotY) {
        offsets.add(IntIntPair.of(this.slotX, this.slotY));
        this.slotX += slotX;
        this.slotY += slotY;

    }

    public void popOffset() {
        IntIntPair offset = offsets.remove(offsets.size() - 1);
        this.slotX = offset.leftInt();
        this.slotY = offset.rightInt();
    }

    public void drawSprite(@NotNull BadSprite sprite, int xOffset, int yOffset) {
        title.pushColor(FontUtil.computeVerticalOffset(yOffset + 4)); // + 4 accounts for the gui title offset
        title.pos(xOffset - 1); // - 1 accounts for the gui title offset
        title.drawInPlace(sprite);
        title.popColor();
    }

    public void drawText(@NotNull String text, int xOffset, int yOffset) {
        title.pushColor(FontUtil.computeVerticalOffset(yOffset + 4)); // + 4 accounts for the gui title offset
        title.pos(xOffset - 1); // - 1 accounts for the gui title offset
        title.append(text);
        title.popColor();
    }

    public void fill(@NotNull ItemStack itemStack) {

    }

    public @NotNull Component getTitleText() {
        return title.build();
    }

    public @NotNull ItemStack[] getItems() {
        return new ItemStack[0];
    }
}
