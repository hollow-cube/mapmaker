package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class ItemPickerView extends View {

    private @Outlet("title") Text titleText;

    private final int index;

    public ItemPickerView(@NotNull Context context, int index) {
        super(context);
        this.index = index;

        titleText.setText("Item Slot #" + (index + 1));
    }
}
